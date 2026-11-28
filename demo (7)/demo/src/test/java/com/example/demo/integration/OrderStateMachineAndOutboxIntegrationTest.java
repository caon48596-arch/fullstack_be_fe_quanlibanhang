package com.example.demo.integration;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.CouponRepository;
import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.OutboxEventRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.Category;
import com.example.demo.entity.Coupon;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.OutboxEvent;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.security.JwtUtil;
import com.example.demo.service.OrderService;
import com.example.demo.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Order State Machine, Stock Compensation, Payment Webhook, Outbox & Security Integration Tests")
public class OrderStateMachineAndOutboxIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrderService orderService;
    @Autowired private OutboxService outboxService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CouponRepository couponRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private User createUser(String email, String role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setUsername(email.split("@")[0]);
            u.setPassword(passwordEncoder.encode("SuperSecret#123"));
            u.setRole(role);
            return userRepository.save(u);
        });
    }

    private Product createProduct(String name, String price, int stock) {
        Category cat = new Category();
        cat.setName("Cat-" + System.nanoTime());
        cat = categoryRepository.save(cat);

        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setCategory(cat);
        return productRepository.save(p);
    }

    @Test
    @DisplayName("Không bao giờ lộ password hash của User trong JSON API Order & Dashboard + chống IDOR")
    void testNoPasswordLeakAndIdorProtection() throws Exception {
        String ownerEmail = "owner_" + System.nanoTime() + "@test.com";
        String otherEmail = "other_" + System.nanoTime() + "@test.com";
        createUser(ownerEmail, "ROLE_CLIENT");
        createUser(otherEmail, "ROLE_CLIENT");
        createUser("admin_leak_check@test.com", "ROLE_ADMIN");

        Product product = createProduct("iPhone 17 Pro", "30000000.00", 5);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        Order order = orderService.placeOrder(List.of(item), null, ownerEmail, "idem-leak-" + System.nanoTime());

        String ownerToken = jwtUtil.generateToken(ownerEmail, "ROLE_CLIENT");
        String otherToken = jwtUtil.generateToken(otherEmail, "ROLE_CLIENT");
        String adminToken = jwtUtil.generateToken("admin_leak_check@test.com", "ROLE_ADMIN");

        // 1. Chủ đơn xem chi tiết đơn hàng -> 200 OK và tuyệt đối KHÔNG có field password
        String orderJson = mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(order.getId().intValue())))
                .andReturn().getResponse().getContentAsString();
        assertFalse(orderJson.contains("password"), "Response Order tuyệt đối không được chứa trường password");
        assertFalse(orderJson.contains("$2a$"), "Response Order tuyệt đối không được chứa BCrypt hash");

        // 2. Người dùng khác truy cập đơn hàng -> 403 Forbidden (Chống IDOR)
        mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        // 3. Dashboard Admin cũng không được lộ password hash
        String dashboardJson = mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(dashboardJson.contains("$2a$"), "Dashboard recentOrders không được lộ BCrypt hash");
    }

    @Test
    @DisplayName("Hủy đơn hàng hoàn tồn kho, hoàn lượt coupon, loại khỏi doanh thu Dashboard và chặn chuyển ngược (409)")
    void testCancelOrder_CompensatesStockCouponExcludesRevenueAndBlocksInvalidTransition() throws Exception {
        String email = "cancel_buyer_" + System.nanoTime() + "@test.com";
        createUser(email, "ROLE_CLIENT");
        createUser("admin_sm@test.com", "ROLE_ADMIN");
        String buyerToken = jwtUtil.generateToken(email, "ROLE_CLIENT");
        String adminToken = jwtUtil.generateToken("admin_sm@test.com", "ROLE_ADMIN");

        Product product = createProduct("MacBook Air M4", "25000000.00", 10);
        Coupon coupon = new Coupon();
        coupon.setCode("ONCE_" + System.nanoTime());
        coupon.setDiscountAmount(new BigDecimal("1000000.00"));
        coupon.setQuantity(1);
        coupon.setActive(true);
        coupon.setExpirationDate(LocalDateTime.now().plusDays(5));
        coupon = couponRepository.save(coupon);

        BigDecimal revenueBefore = orderRepository.sumTotalRevenue();

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(3);
        Order order = orderService.placeOrder(List.of(item), coupon.getCode(), email, "idem-cancel-" + System.nanoTime());

        // Sau đặt hàng: tồn kho giảm còn 7, coupon hết lượt & inactive, doanh thu tăng 74M
        assertEquals(7, productRepository.findById(product.getId()).orElseThrow().getStock());
        Coupon afterOrderCoupon = couponRepository.findByCode(coupon.getCode()).orElseThrow();
        assertEquals(0, afterOrderCoupon.getQuantity());
        assertFalse(afterOrderCoupon.isActive());
        assertEquals(0, revenueBefore.add(new BigDecimal("74000000.00")).compareTo(orderRepository.sumTotalRevenue()));

        // Hủy đơn hàng qua API POST /api/orders/{id}/cancel
        mockMvc.perform(post("/api/orders/" + order.getId() + "/cancel")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));

        // Kiểm chứng hoàn tồn kho (7 -> 10) & hoàn coupon (0 -> 1, active = true)
        assertEquals(10, productRepository.findById(product.getId()).orElseThrow().getStock());
        Coupon restoredCoupon = couponRepository.findByCode(coupon.getCode()).orElseThrow();
        assertEquals(1, restoredCoupon.getQuantity());
        assertTrue(restoredCoupon.isActive());

        // Doanh thu Dashboard tự động loại trừ đơn CANCELLED
        assertEquals(0, revenueBefore.compareTo(orderRepository.sumTotalRevenue()));

        // State Machine chặn chuyển ngược từ CANCELLED sang PAID (HTTP 409 Conflict)
        mockMvc.perform(put("/api/orders/" + order.getId() + "/status")
                        .param("status", "PAID")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("409")));
    }

    @Test
    @DisplayName("Payment Webhook HMAC-SHA256: từ chối chữ ký sai (401), cập nhật PAID idempotent và tạo Outbox Event")
    void testPaymentWebhook_SignatureVerificationAndIdempotency() throws Exception {
        String email = "webhook_buyer_" + System.nanoTime() + "@test.com";
        createUser(email, "ROLE_CLIENT");
        Product product = createProduct("iPad Pro M4", "28000000.00", 6);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        Order order = orderService.placeOrder(List.of(item), null, email, "idem-wh-" + System.nanoTime());

        // Kiểm chứng Transactional Outbox đã ghi sự kiện ORDER_PLACED cùng transaction
        List<OutboxEvent> outboxEvents = outboxEventRepository.findByAggregateTypeAndAggregateId("ORDER", order.getId());
        assertEquals(1, outboxEvents.size(), "Phải có đúng 1 bản ghi Outbox ORDER_PLACED");
        assertEquals(OutboxEvent.EVENT_ORDER_PLACED, outboxEvents.get(0).getEventType());
        outboxService.relayPendingEvents(20);

        String txId = "VNPAY-TX-" + System.nanoTime();
        Map<String, Object> webhookBody = Map.of(
                "orderId", order.getId(),
                "transactionId", txId,
                "status", "SUCCESS",
                "amount", new BigDecimal("28000000.00"),
                "provider", "VNPAY"
        );

        // 1. Sai chữ ký HMAC -> 401 Unauthorized
        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Payment-Signature", "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookBody)))
                .andExpect(status().isUnauthorized());

        // 2. Đúng chữ ký HMAC -> 200 OK, đơn chuyển sang PAID
        String validSig = orderService.computeWebhookSignature(order.getId(), txId, "SUCCESS");
        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Payment-Signature", validSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PAID")))
                .andExpect(jsonPath("$.data.paymentStatus", is("PAID")));

        // 3. Gọi lại webhook trùng transactionId (Idempotent retry) -> vẫn 200 OK, không báo lỗi 409
        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Payment-Signature", validSig)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PAID")));
    }

    @Test
    @DisplayName("Scheduler tự động hết hạn đơn PENDING quá 15 phút (EXPIRED) và hoàn tồn kho")
    void testExpireUnpaidOrders_RestoresStock() {
        String email = "expire_buyer_" + System.nanoTime() + "@test.com";
        createUser(email, "ROLE_CLIENT");
        Product product = createProduct("AirPods Pro 3", "6000000.00", 8);

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        Order order = orderService.placeOrder(List.of(item), null, email, "idem-exp-" + System.nanoTime());
        assertEquals(6, productRepository.findById(product.getId()).orElseThrow().getStock());

        // Giả lập đơn đã quá hạn thanh toán 20 phút trước
        order.setExpiresAt(LocalDateTime.now().minusMinutes(20));
        orderRepository.save(order);

        int expiredCount = orderService.expireUnpaidOrders();
        assertTrue(expiredCount >= 1);

        Order expiredOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.EXPIRED.name(), expiredOrder.getStatus());
        assertEquals(8, productRepository.findById(product.getId()).orElseThrow().getStock());
    }
}
