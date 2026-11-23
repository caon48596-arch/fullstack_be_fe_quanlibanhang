package com.example.demo.Unit_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        sampleProduct.setPrice(100.0);
        sampleProduct.setStock(20);

        sampleItem = new OrderItem();
        sampleItem.setId(100L);
        sampleItem.setProduct(sampleProduct);
        sampleItem.setQuantity(2);
        sampleItem.setPrice(100.0);
    }

    @Test
    @DisplayName("Đặt hàng thành công không có coupon")
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
        assertEquals(200.0, order.getTotalAmount());
        assertEquals("PENDING", order.getStatus());
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
        coupon.setDiscountAmount(50.0);
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
        assertEquals(150.0, order.getTotalAmount());
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
        expiredCoupon.setDiscountAmount(20.0);
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
    @DisplayName("Cập nhật trạng thái đơn hàng thành công")
    void testUpdateOrderStatus_Success() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(1L, "SHIPPED");

        assertEquals("SHIPPED", updated.getStatus());
        verify(orderRepository, times(1)).save(order);
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
}
