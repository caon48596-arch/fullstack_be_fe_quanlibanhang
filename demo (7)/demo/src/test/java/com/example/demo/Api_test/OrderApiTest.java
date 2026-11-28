package com.example.demo.Api_test;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.OrderItemRepository;
import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.entity.Category;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API Test - Order & OrderItem Service")
public class OrderApiTest extends BaseApiTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product sampleProduct;
    private Order sampleOrder;

    @BeforeEach
    void setUpOrder() {
        Category category = new Category();
        category.setName("Danh Mục Cho Order " + System.currentTimeMillis());
        category = categoryRepository.save(category);

        sampleProduct = new Product();
        sampleProduct.setName("Sản Phẩm Đặt Hàng API");
        sampleProduct.setPrice(new BigDecimal("100000.00"));
        sampleProduct.setStock(20);
        sampleProduct.setCategory(category);
        sampleProduct = productRepository.save(sampleProduct);

        sampleOrder = new Order();
        sampleOrder.setUser(clientUser);
        sampleOrder.setOrderDate(LocalDateTime.now());
        sampleOrder.setTotalAmount(new BigDecimal("200000.00"));
        sampleOrder.setStatus("PENDING");
        sampleOrder = orderRepository.save(sampleOrder);
    }

    @Test
    @DisplayName("POST /api/orders/CreateAndSum - Khách hàng đặt hàng thành công (Authenticated)")
    void testPlaceOrder_Success() throws Exception {
        Map<String, Object> body = Map.of(
                "email", clientUser.getEmail(),
                "items", List.of(
                        Map.of("product", Map.of("id", sampleProduct.getId()), "quantity", 2)
                )
        );

        mockMvc.perform(post("/api/orders/CreateAndSum")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data.id", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/orders/CreateAndSum - Idempotency-Key chống tạo đơn trùng lặp")
    void testPlaceOrder_Idempotent_WithHeader() throws Exception {
        String idempotencyKey = "test-idem-" + System.currentTimeMillis();
        int stockBefore = sampleProduct.getStock();

        Map<String, Object> body = Map.of(
                "email", clientUser.getEmail(),
                "items", List.of(
                        Map.of("product", Map.of("id", sampleProduct.getId()), "quantity", 2)
                )
        );

        String firstResponse = mockMvc.perform(post("/api/orders/CreateAndSum")
                        .header("Authorization", "Bearer " + clientToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/orders/CreateAndSum")
                        .header("Authorization", "Bearer " + clientToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long firstOrderId = objectMapper.readTree(firstResponse).path("data").path("id").asLong();
        Long secondOrderId = objectMapper.readTree(secondResponse).path("data").path("id").asLong();

        org.junit.jupiter.api.Assertions.assertEquals(firstOrderId, secondOrderId);
        org.junit.jupiter.api.Assertions.assertEquals(
                stockBefore - 2,
                productRepository.findById(sampleProduct.getId()).orElseThrow().getStock());
    }

    @Test
    @DisplayName("POST /api/orders/CreateAndSum - Từ chối khi không có Token xác thực (401/403)")
    void testPlaceOrder_Unauthorized() throws Exception {
        Map<String, Object> body = Map.of(
                "email", "khong_token@test.com",
                "items", List.of(
                        Map.of("product", Map.of("id", sampleProduct.getId()), "quantity", 1)
                )
        );

        mockMvc.perform(post("/api/orders/CreateAndSum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/orders/my-orders - Khách hàng xem danh sách đơn hàng của mình")
    void testGetMyOrders_Success() throws Exception {
        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/orders/my-order-items - Khách hàng xem danh sách sản phẩm trong các đơn của mình")
    void testGetMyOrderItems_Success() throws Exception {
        mockMvc.perform(get("/api/orders/my-order-items")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/orders/ReadOne/{id} - Xem chi tiết 1 đơn hàng theo ID")
    void testGetOrderById_Success() throws Exception {
        mockMvc.perform(get("/api/orders/ReadOne/" + sampleOrder.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data.id", is(sampleOrder.getId().intValue())));
    }

    @Test
    @DisplayName("GET /api/orders/ReadAll - ADMIN xem toàn bộ danh sách món đã đặt trong hệ thống")
    void testGetAllOrderItems_AdminSuccess() throws Exception {
        mockMvc.perform(get("/api/orders/ReadAll")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/orders/ReadAll - CLIENT bị chặn khi truy cập (403 Forbidden)")
    void testGetAllOrderItems_ClientForbidden() throws Exception {
        mockMvc.perform(get("/api/orders/ReadAll")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - ADMIN cập nhật trạng thái đơn hàng (CONFIRMED)")
    void testUpdateOrderStatus_AdminSuccess() throws Exception {
        mockMvc.perform(put("/api/orders/" + sampleOrder.getId() + "/status")
                        .param("status", "CONFIRMED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("PUT /api/orders/item/{id}/status - ADMIN cập nhật trạng thái món hàng (SHIPPED)")
    void testUpdateOrderItemStatus_AdminSuccess() throws Exception {
        OrderItem item = new OrderItem();
        item.setOrder(sampleOrder);
        item.setProduct(sampleProduct);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("100000.00"));
        item.setStatus("PENDING");
        item = orderItemRepository.save(item);

        mockMvc.perform(put("/api/orders/item/" + item.getId() + "/status")
                        .param("status", "SHIPPED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("PUT /api/order-items/{id} - Cập nhật số lượng của Order Item")
    void testUpdateOrderItemQuantity_Success() throws Exception {
        OrderItem item = new OrderItem();
        item.setOrder(sampleOrder);
        item.setProduct(sampleProduct);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("100000.00"));
        item = orderItemRepository.save(item);

        Map<String, Object> body = Map.of("quantity", 5);

        mockMvc.perform(put("/api/order-items/" + item.getId())
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(5)));
    }

    @Test
    @DisplayName("DELETE /api/order-items/{id} - Xóa một món hàng khỏi đơn hàng")
    void testDeleteOrderItem_Success() throws Exception {
        OrderItem item = new OrderItem();
        item.setOrder(sampleOrder);
        item.setProduct(sampleProduct);
        item.setQuantity(1);
        item.setPrice(new BigDecimal("100000.00"));
        item = orderItemRepository.save(item);

        mockMvc.perform(delete("/api/order-items/" + item.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());

        assertTrue(orderItemRepository.findById(item.getId()).isEmpty());
    }
}
