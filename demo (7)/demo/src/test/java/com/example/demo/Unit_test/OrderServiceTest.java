package com.example.demo.Unit_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Repository.CouponRepository;
import com.example.demo.Repository.OrderItemRepository;
import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.Coupon;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.exception.ConflictException;
import com.example.demo.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User sampleUser;
    private Product sampleProduct;
    private OrderItem sampleItem;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("buyer@example.com");

        sampleProduct = new Product();
        sampleProduct.setId(10L);
        sampleProduct.setName("Bàn phím cơ");
        sampleProduct.setPrice(new BigDecimal("100.00"));
        sampleProduct.setStock(20);

        sampleItem = new OrderItem();
        sampleItem.setId(100L);
        sampleItem.setProduct(sampleProduct);
        sampleItem.setQuantity(2);
        sampleItem.setPrice(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Đặt hàng thành công không có coupon (tính toán chuẩn BigDecimal)")
    void testPlaceOrder_Success_WithoutCoupon() {
        List<OrderItem> items = new ArrayList<>();
        items.add(sampleItem);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.placeOrder(null, items, null, "buyer@example.com");

        assertNotNull(order);
        assertEquals(0, new BigDecimal("200.00").compareTo(order.getTotalAmount()));
        assertEquals("PENDING", order.getStatus());
        assertEquals("UNPAID", order.getPaymentStatus());
        assertNotNull(order.getExpiresAt());
        assertEquals(18, sampleProduct.getStock());
        assertEquals(sampleUser, order.getUser());
        verify(productRepository, times(1)).save(sampleProduct);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderItemRepository, times(1)).save(sampleItem);
    }

    @Test
    @DisplayName("Đặt hàng thành công có mã giảm giá hợp lệ")
    void testPlaceOrder_Success_WithValidCoupon() {
        List<OrderItem> items = new ArrayList<>();
        items.add(sampleItem);

        Coupon coupon = new Coupon();
        coupon.setId(5L);
        coupon.setCode("GIAM50");
        coupon.setDiscountAmount(new BigDecimal("50.00"));
        coupon.setQuantity(10);
        coupon.setActive(true);
        coupon.setExpirationDate(LocalDateTime.now().plusDays(1));

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleProduct));
        when(couponRepository.findByCodeForUpdate("GIAM50")).thenReturn(Optional.of(coupon));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.placeOrder(null, items, "GIAM50", "buyer@example.com");

        assertNotNull(order);
        assertEquals(0, new BigDecimal("150.00").compareTo(order.getTotalAmount()));
        assertEquals("GIAM50", order.getCouponCode());
        assertEquals(9, coupon.getQuantity());
        verify(couponRepository, times(1)).save(coupon);
    }

    @Test
    @DisplayName("Đặt hàng thất bại khi sản phẩm không tồn tại")
    void testPlaceOrder_ProductNotFound_ThrowsException() {
        List<OrderItem> items = new ArrayList<>();
        items.add(sampleItem);

        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(null, items, null, "buyer@example.com");
        });

        assertTrue(exception.getMessage().contains("sản phẩm k tồn tại"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Đặt hàng thất bại khi tồn kho không đủ")
    void testPlaceOrder_InsufficientStock_ThrowsException() {
        sampleProduct.setStock(1);
        List<OrderItem> items = new ArrayList<>();
        items.add(sampleItem);

        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleProduct));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(null, items, null, "buyer@example.com");
        });

        assertTrue(exception.getMessage().contains("không đủ số lượng trong kho"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Đặt hàng thất bại khi mã giảm giá không tồn tại")
    void testPlaceOrder_InvalidCoupon_ThrowsException() {
        List<OrderItem> items = new ArrayList<>();
        items.add(sampleItem);

        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleProduct));
        when(couponRepository.findByCodeForUpdate("INVALID")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(null, items, "INVALID", "buyer@example.com");
        });

        assertTrue(exception.getMessage().contains("Mã giảm giá không tồn tại"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Đặt hàng thất bại khi mã giảm giá đã hết hạn")
    void testPlaceOrder_ExpiredCoupon_ThrowsException() {
        List<OrderItem> items = new ArrayList<>();
        items.add(sampleItem);

        Coupon expiredCoupon = new Coupon();
        expiredCoupon.setCode("EXPIRED");
        expiredCoupon.setActive(true);
        expiredCoupon.setQuantity(5);
        expiredCoupon.setDiscountAmount(new BigDecimal("20.00"));
        expiredCoupon.setExpirationDate(LocalDateTime.now().minusDays(1));

        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleProduct));
        when(couponRepository.findByCodeForUpdate("EXPIRED")).thenReturn(Optional.of(expiredCoupon));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(null, items, "EXPIRED", "buyer@example.com");
        });

        assertTrue(exception.getMessage().contains("hết hạn sử dụng"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Đặt hàng thất bại khi mã giảm giá đã hết lượt dùng hoặc inactive")
    void testPlaceOrder_ExhaustedCoupon_ThrowsException() {
        List<OrderItem> items = new ArrayList<>();
        items.add(sampleItem);

        Coupon exhaustedCoupon = new Coupon();
        exhaustedCoupon.setCode("HETLUOT");
        exhaustedCoupon.setActive(false);
        exhaustedCoupon.setQuantity(0);

        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleProduct));
        when(couponRepository.findByCodeForUpdate("HETLUOT")).thenReturn(Optional.of(exhaustedCoupon));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(null, items, "HETLUOT", "buyer@example.com");
        });

        assertTrue(exception.getMessage().contains("hết lượt sử dụng hoặc không hợp lệ"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Xem chi tiết đơn hàng theo ID thành công")
    void testReadOne_Success() {
        Order order = new Order();
        order.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Xem chi tiết đơn hàng theo ID không tìm thấy")
    void testReadOne_NotFound_ThrowsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(999L);
        });
    }

    @Test
    @DisplayName("Lấy danh sách tất cả món hàng đặt")
    void testReadAll_Success() {
        when(orderItemRepository.findAll()).thenReturn(List.of(sampleItem));

        List<OrderItem> items = orderService.getAllOrderItems();

        assertNotNull(items);
        assertEquals(1, items.size());
    }

    @Test
    @DisplayName("Lấy danh sách đơn hàng theo email khách")
    void testGetOrdersByUserEmail_Success() {
        Order order = new Order();
        order.setId(1L);
        when(orderRepository.findByUser_Email("buyer@example.com")).thenReturn(List.of(order));

        List<Order> orders = orderService.getOrdersByUserEmail("buyer@example.com");

        assertNotNull(orders);
        assertEquals(1, orders.size());
    }

    @Test
    @DisplayName("Lấy danh sách chi tiết đơn hàng theo email khách")
    void testGetOrderItemsByUserEmail_Success() {
        when(orderItemRepository.findByOrder_User_Email("buyer@example.com")).thenReturn(List.of(sampleItem));

        List<OrderItem> items = orderService.getOrderItemsByUserEmail("buyer@example.com");

        assertNotNull(items);
        assertEquals(1, items.size());
    }

    @Test
    @DisplayName("State Machine: Cập nhật trạng thái đơn hàng hợp lệ (PENDING -> SHIPPED)")
    void testUpdateOrderStatus_Success() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(1L, "SHIPPED");

        assertEquals("SHIPPED", updated.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("State Machine: Chặn chuyển trạng thái sai nghiệp vụ (CANCELLED -> PAID) bằng 409 Conflict")
    void testUpdateOrderStatus_InvalidTransition_ThrowsConflict() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("CANCELLED");

        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThrows(ConflictException.class, () -> orderService.updateOrderStatus(1L, "PAID"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Cập nhật trạng thái mục đơn hàng thành công")
    void testUpdateOrderItemStatus_Success() {
        when(orderItemRepository.findById(100L)).thenReturn(Optional.of(sampleItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderItem updated = orderService.updateOrderItemStatus(100L, "COMPLETED");

        assertEquals("COMPLETED", updated.getStatus());
        verify(orderItemRepository, times(1)).save(sampleItem);
    }

    @Test
    @DisplayName("Idempotency Key trùng lặp trả về đơn hàng đã tạo, không trừ kho lần 2")
    void testPlaceOrder_IdempotencyKey_ReturnsExistingOrder() {
        Order existing = new Order();
        existing.setId(77L);
        existing.setIdempotencyKey("idem-key-123");
        existing.setTotalAmount(new BigDecimal("200.00"));

        when(orderRepository.findByIdempotencyKey("idem-key-123")).thenReturn(Optional.of(existing));

        Order result = orderService.placeOrder(List.of(sampleItem), null, "buyer@example.com", "idem-key-123");

        assertEquals(77L, result.getId());
        verify(productRepository, never()).findByIdForUpdate(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Chống Deadlock: luôn khóa sản phẩm theo thứ tự productId tăng dần")
    void testPlaceOrder_LocksProductsInAscendingIdOrder() {
        Product p5 = new Product();
        p5.setId(5L);
        p5.setName("Chuột");
        p5.setPrice(new BigDecimal("50.00"));
        p5.setStock(10);

        Product p20 = new Product();
        p20.setId(20L);
        p20.setName("Màn hình");
        p20.setPrice(new BigDecimal("300.00"));
        p20.setStock(10);

        OrderItem item20 = new OrderItem();
        item20.setProduct(p20);
        item20.setQuantity(1);

        OrderItem item5 = new OrderItem();
        item5.setProduct(p5);
        item5.setQuantity(2);

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(productRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(p5));
        when(productRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(p20));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        // Client truyền ngược [20, 5]
        Order order = orderService.placeOrder(List.of(item20, item5), null, "buyer@example.com", null);

        assertEquals(0, new BigDecimal("400.00").compareTo(order.getTotalAmount()));
        InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).findByIdForUpdate(5L);
        inOrder.verify(productRepository).findByIdForUpdate(20L);
    }

    @Test
    @DisplayName("Đặt hàng thất bại nếu sản phẩm đã bị xóa mềm (Soft Delete)")
    void testPlaceOrder_DeletedProduct_ThrowsException() {
        sampleProduct.setDeleted(true);
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(sampleProduct));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                orderService.placeOrder(List.of(sampleItem), null, "buyer@example.com", null));

        assertTrue(ex.getMessage().contains("ngừng kinh doanh"));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
