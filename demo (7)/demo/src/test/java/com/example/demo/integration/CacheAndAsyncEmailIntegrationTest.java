package com.example.demo.integration;

import com.example.demo.Repository.CategoryRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.config.CacheConfig;
import com.example.demo.entity.Category;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.event.OrderPlacedEvent;
import com.example.demo.service.CategoryService;
import com.example.demo.service.DashboardService;
import com.example.demo.service.EmailService;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("Cache (Redis/Caffeine) Invalidation & Async Email Integration Tests")
public class CacheAndAsyncEmailIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @SpyBean
    private EmailService emailService;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        reset(emailService);
    }

    @Test
    @DisplayName("Cache Product Detail & Dashboard Stats được lưu và tự động XÓA khi Admin sửa sản phẩm")
    void testProductDetailAndDashboardCache_EvictedOnAdminUpdate() {
        Category cat = new Category();
        cat.setName("Cache Cat " + System.currentTimeMillis());
        cat = categoryRepository.save(cat);

        Product created = new Product();
        created.setName("MacBook Cache Original");
        created.setPrice(new BigDecimal("30000000.00"));
        created.setStock(10);
        created.setCategory(cat);
        created = productService.createProduct(created);
        Long productId = created.getId();

        // 1. Nạp cache productDetail, categories và dashboardStats
        Product cachedFirst = productService.getProductById(productId);
        assertEquals("MacBook Cache Original", cachedFirst.getName());
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAIL).get(productId));

        categoryService.getAllCategories();
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_CATEGORIES).get("all"));

        dashboardService.getDashboardStats();
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS).get("overview"));

        // 2. Admin sửa sản phẩm -> cache productDetail(id) và dashboardStats phải bị xóa ngay
        Product updatePayload = new Product();
        updatePayload.setName("MacBook Cache Updated");
        updatePayload.setPrice(new BigDecimal("28500000.00"));
        updatePayload.setStock(8);
        productService.updateProduct(productId, updatePayload);

        assertNull(cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAIL).get(productId),
                "Cache productDetail phải bị xóa sau khi Admin sửa sản phẩm");
        assertNull(cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS).get("overview"),
                "Cache dashboardStats phải bị xóa sau khi Admin sửa sản phẩm");

        // 3. Đọc lại phải trả dữ liệu mới nhất từ DB
        Product refreshed = productService.getProductById(productId);
        assertEquals("MacBook Cache Updated", refreshed.getName());
        assertEquals(0, new BigDecimal("28500000.00").compareTo(refreshed.getPrice()));
    }

    @Test
    @DisplayName("Đặt hàng thành công: tự xóa cache sản phẩm/dashboard và bắn Email Hóa đơn bất đồng bộ (AFTER_COMMIT)")
    void testPlaceOrder_EvictsCacheAndTriggersAsyncInvoiceEmail() {
        String buyerEmail = "async_buyer_" + System.currentTimeMillis() + "@example.com";
        User buyer = new User();
        buyer.setEmail(buyerEmail);
        buyer.setUsername("Khach Async");
        buyer.setPassword(passwordEncoder.encode("secret123"));
        buyer.setRole("ROLE_CLIENT");
        userRepository.save(buyer);

        Category cat = new Category();
        cat.setName("Async Cat " + System.currentTimeMillis());
        cat = categoryRepository.save(cat);

        Product p = new Product();
        p.setName("iPhone 16 Pro Async");
        p.setPrice(new BigDecimal("29000000.00"));
        p.setStock(5);
        p.setCategory(cat);
        p = productRepository.save(p);

        // Nạp sẵn cache trước khi đặt hàng
        productService.getProductById(p.getId());
        dashboardService.getDashboardStats();
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAIL).get(p.getId()));
        assertNotNull(cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS).get("overview"));

        OrderItem item = new OrderItem();
        item.setProduct(p);
        item.setQuantity(1);

        Order savedOrder = orderService.placeOrder(List.of(item), null, buyerEmail, "idem-async-" + System.currentTimeMillis());
        assertNotNull(savedOrder.getId());

        // Cache phải bị xóa do tồn kho & doanh thu thay đổi
        assertNull(cacheManager.getCache(CacheConfig.CACHE_PRODUCT_DETAIL).get(p.getId()),
                "Cache productDetail phải bị xóa sau khi trừ tồn kho");
        assertNull(cacheManager.getCache(CacheConfig.CACHE_DASHBOARD_STATS).get("overview"),
                "Cache dashboardStats phải bị xóa sau khi có đơn hàng mới");

        // Xác minh luồng @Async gửi Email hóa đơn được kích hoạt sau khi commit DB
        ArgumentCaptor<OrderPlacedEvent> captor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(emailService, timeout(3000).times(1)).sendOrderInvoiceEmail(captor.capture());

        OrderPlacedEvent sentEvent = captor.getValue();
        assertEquals(savedOrder.getId(), sentEvent.orderId());
        assertEquals(buyerEmail, sentEvent.customerEmail());
        assertEquals(0, new BigDecimal("29000000.00").compareTo(sentEvent.totalAmount()));
        assertEquals(1, sentEvent.items().size());
    }

    @Test
    @DisplayName("Đặt hàng thất bại (rollback) KHÔNG gửi nhầm Email Hóa đơn")
    void testPlaceOrder_Rollback_DoesNotSendInvoiceEmail() {
        Category cat = new Category();
        cat.setName("Rollback Cat " + System.currentTimeMillis());
        cat = categoryRepository.save(cat);

        Product p = new Product();
        p.setName("Sản phẩm hết hàng");
        p.setPrice(new BigDecimal("1000000.00"));
        p.setStock(1);
        p.setCategory(cat);
        p = productRepository.save(p);

        OrderItem item = new OrderItem();
        item.setProduct(p);
        item.setQuantity(99);

        assertThrows(RuntimeException.class, () ->
                orderService.placeOrder(List.of(item), null, "rollback_user@example.com", null));

        verify(emailService, after(300).never()).sendOrderInvoiceEmail(any());
    }
}
