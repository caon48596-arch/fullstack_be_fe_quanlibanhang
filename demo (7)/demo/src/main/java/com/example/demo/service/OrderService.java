package com.example.demo.service;

import com.example.demo.Repository.CouponRepository;
import com.example.demo.Repository.OrderItemRepository;
import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.config.CacheConfig;
import com.example.demo.dto.OrderDTO.PaymentWebhookDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.OutboxEvent;
import com.example.demo.entity.Product;
import com.example.demo.event.OrderPlacedEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.cache.CatalogCacheEvictor;
import com.example.demo.service.contract.CouponOperations;
import com.example.demo.service.contract.CouponOperations.AppliedCoupon;
import com.example.demo.service.contract.OrderItemOperations;
import com.example.demo.service.contract.OrderOperations;
import com.example.demo.service.contract.OutboxOperations;
import com.example.demo.service.discount.FixedAmountCouponDiscountPolicy;
import com.example.demo.service.payment.HmacSha256PaymentSignatureVerifier;
import com.example.demo.service.payment.PaymentSignatureVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Nghiệp vụ cốt lõi vòng đời Đơn hàng (SRP, OCP, ISP, DIP):
 * - Tách Coupon sang {@link CouponOperations}
 * - Tách OrderItem sang {@link OrderItemOperations}
 * - Tách Payment Webhook & HMAC sang {@link PaymentWebhookService} / {@link PaymentSignatureVerifier}
 * - Tách quản lý Cache sang {@link CatalogCacheEvictor}
 * - Tách lập lịch định kỳ sang {@link com.example.demo.service.scheduler.OrderExpirationScheduler}
 */
@Service
@Transactional
@Slf4j
public class OrderService implements OrderOperations {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CouponOperations couponOperations;
    private final OrderItemOperations orderItemOperations;
    private final CatalogCacheEvictor catalogCacheEvictor;
    private final OutboxOperations outboxOperations;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentSignatureVerifier signatureVerifier;

    @Value("${app.orders.payment-timeout-minutes:15}")
    private long paymentTimeoutMinutes = 15L;

    @Autowired
    public OrderService(
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            CouponRepository couponRepository,
            UserRepository userRepository,
            @Autowired(required = false) CouponOperations couponOperations,
            @Autowired(required = false) OrderItemOperations orderItemOperations,
            @Autowired(required = false) CatalogCacheEvictor catalogCacheEvictor,
            @Autowired(required = false) OutboxOperations outboxOperations,
            @Autowired(required = false) ApplicationEventPublisher eventPublisher,
            @Autowired(required = false) PaymentSignatureVerifier signatureVerifier) {
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.couponOperations = couponOperations != null
                ? couponOperations
                : (couponRepository != null
                        ? new CouponService(couponRepository, List.of(new FixedAmountCouponDiscountPolicy()))
                        : null);
        this.orderItemOperations = orderItemOperations != null
                ? orderItemOperations
                : (orderItemRepository != null ? new OrderItemService(orderItemRepository) : null);
        this.catalogCacheEvictor = catalogCacheEvictor;
        this.outboxOperations = outboxOperations;
        this.eventPublisher = eventPublisher;
        this.signatureVerifier = signatureVerifier != null
                ? signatureVerifier
                : new HmacSha256PaymentSignatureVerifier();
    }

    public Order placeOrder(Long ignoredClientId, List<OrderItem> items, String couponCode, String email) {
        return placeOrder(items, couponCode, email, null);
    }

    @Override
    public Order placeOrder(List<OrderItem> items, String couponCode, String email, String idempotencyKey) {
        String normalizedIdempotencyKey = (idempotencyKey != null && !idempotencyKey.trim().isEmpty())
                ? idempotencyKey.trim()
                : null;

        if (normalizedIdempotencyKey != null) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(normalizedIdempotencyKey);
            if (existingOrder.isPresent()) {
                return existingOrder.get();
            }
        }

        if (items == null || items.isEmpty()) {
            throw new BusinessException("Giỏ hàng trống, không thể tạo đơn hàng");
        }

        List<OrderItem> sortedItems = validateAndSortOrderItems(items);

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setOrderDate(now);
        order.setIdempotencyKey(normalizedIdempotencyKey);
        order.setPaymentStatus("UNPAID");
        order.setExpiresAt(now.plusMinutes(paymentTimeoutMinutes > 0 ? paymentTimeoutMinutes : 15L));

        if (email != null && !email.isEmpty()) {
            userRepository.findByEmail(email).ifPresent(order::setUser);
        }

        Map<Long, Product> lockedProducts = new HashMap<>();
        BigDecimal totalAmount = reserveStockAndPriceItems(sortedItems, lockedProducts);

        String appliedCouponCode = null;
        if (couponOperations != null && couponCode != null && !couponCode.trim().isEmpty()) {
            AppliedCoupon applied = couponOperations.applyCouponForOrder(couponCode, totalAmount, now);
            appliedCouponCode = applied.code();
            totalAmount = applied.discountedTotal();
        }

        order.setCouponCode(appliedCouponCode);
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.PENDING.name());
        Order savedOrder = orderRepository.save(order);

        List<OrderPlacedEvent.OrderItemSnapshot> itemSnapshots = persistOrderItems(savedOrder, sortedItems);

        if (catalogCacheEvictor != null) {
            catalogCacheEvictor.evictAfterOrderChange(lockedProducts.keySet());
        }

        publishOrderPlacedDomainEvent(savedOrder, email, appliedCouponCode, itemSnapshots);

        return savedOrder;
    }

    private List<OrderItem> validateAndSortOrderItems(List<OrderItem> items) {
        List<OrderItem> sortedItems = new ArrayList<>(items);
        for (OrderItem item : sortedItems) {
            if (item == null || item.getProduct() == null || item.getProduct().getId() == null) {
                throw new BusinessException("Thông tin sản phẩm không hợp lệ");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("Số lượng sản phẩm phải lớn hơn 0");
            }
        }
        sortedItems.sort(Comparator.comparing(item -> item.getProduct().getId()));
        return sortedItems;
    }

    private BigDecimal reserveStockAndPriceItems(List<OrderItem> sortedItems, Map<Long, Product> lockedProducts) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem orderItem : sortedItems) {
            Long productId = orderItem.getProduct().getId();
            Product product = lockedProducts.computeIfAbsent(productId, id ->
                    productRepository.findByIdForUpdate(id)
                            .orElseThrow(() -> new ResourceNotFoundException("sản phẩm k tồn tại")));

            if (product.isDeleted()) {
                throw new BusinessException("Sản phẩm " + product.getName() + " đã ngừng kinh doanh");
            }

            if (product.getStock() != null) {
                if (product.getStock() < orderItem.getQuantity()) {
                    throw new BusinessException("Sản phẩm " + product.getName() + " không đủ số lượng trong kho");
                }
                product.setStock(product.getStock() - orderItem.getQuantity());
                productRepository.save(product);
            }

            BigDecimal unitPrice = product.getPrice() != null
                    ? product.getPrice().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            orderItem.setProduct(product);
            orderItem.setPrice(unitPrice);
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }
        return totalAmount;
    }

    private List<OrderPlacedEvent.OrderItemSnapshot> persistOrderItems(Order savedOrder, List<OrderItem> sortedItems) {
        List<OrderPlacedEvent.OrderItemSnapshot> itemSnapshots = new ArrayList<>();
        for (OrderItem item : sortedItems) {
            item.setOrder(savedOrder);
            item.setStatus(OrderStatus.PENDING.name());
            orderItemRepository.save(item);
            itemSnapshots.add(new OrderPlacedEvent.OrderItemSnapshot(
                    item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm",
                    item.getQuantity() != null ? item.getQuantity() : 1,
                    item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO
            ));
        }
        return itemSnapshots;
    }

    private void publishOrderPlacedDomainEvent(
            Order savedOrder,
            String email,
            String appliedCouponCode,
            List<OrderPlacedEvent.OrderItemSnapshot> itemSnapshots) {
        if (email == null || !email.contains("@")) {
            return;
        }
        String customerName = savedOrder.getUser() != null && savedOrder.getUser().getUsername() != null
                ? savedOrder.getUser().getUsername()
                : email;
        OrderPlacedEvent baseEvent = new OrderPlacedEvent(
                savedOrder.getId(),
                email,
                customerName,
                savedOrder.getOrderDate(),
                savedOrder.getTotalAmount(),
                appliedCouponCode,
                itemSnapshots
        );

        Long outboxEventId = null;
        if (outboxOperations != null) {
            OutboxEvent outbox = outboxOperations.recordOrderPlacedEvent(baseEvent);
            outboxEventId = outbox.getId();
        }

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new OrderPlacedEvent(
                    outboxEventId,
                    baseEvent.orderId(),
                    baseEvent.customerEmail(),
                    baseEvent.customerName(),
                    baseEvent.orderDate(),
                    baseEvent.totalAmount(),
                    baseEvent.couponCode(),
                    baseEvent.items()
            ));
        }
    }

    @Override
    @CacheEvict(value = CacheConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    public Order updateOrderStatus(Long orderId, String targetStatusStr) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .or(() -> orderRepository.findById(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        OrderStatus currentStatus = OrderStatus.fromString(
                order.getStatus() != null ? order.getStatus() : OrderStatus.PENDING.name());
        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.fromString(targetStatusStr);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ex.getMessage());
        }

        if (currentStatus == targetStatus) {
            return order;
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new ConflictException(
                    "Không thể chuyển trạng thái đơn hàng #" + orderId + " từ " + currentStatus + " sang " + targetStatus);
        }

        order.setStatus(targetStatus.name());
        if (targetStatus == OrderStatus.PAID) {
            order.setPaymentStatus("PAID");
            order.setExpiresAt(null);
        } else if (targetStatus == OrderStatus.CONFIRMED || targetStatus == OrderStatus.APPROVED) {
            order.setExpiresAt(null);
        }

        Set<Long> compensatedProductIds = new HashSet<>();
        if (targetStatus.requiresStockCompensation()) {
            order.setExpiresAt(null);
            if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                order.setPaymentStatus("REFUNDED");
            }
            compensatedProductIds = compensateOrderResources(order, targetStatus.name());
        }

        Order saved = orderRepository.save(order);
        if (!compensatedProductIds.isEmpty() && catalogCacheEvictor != null) {
            catalogCacheEvictor.evictAfterOrderChange(compensatedProductIds);
        }
        return saved;
    }

    @Override
    public Order cancelOrder(Long orderId, String requesterEmail, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + orderId));

        if (!isAdmin) {
            String ownerEmail = order.getUser() != null ? order.getUser().getEmail() : null;
            if (ownerEmail == null || requesterEmail == null || !ownerEmail.equalsIgnoreCase(requesterEmail)) {
                throw new ForbiddenException("Bạn không có quyền hủy đơn hàng của người khác.");
            }
        }
        return updateOrderStatus(orderId, OrderStatus.CANCELLED.name());
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderForRequester(Long orderId, String requesterEmail, boolean isAdmin) {
        Order order = getOrderById(orderId);
        if (!isAdmin) {
            String ownerEmail = order.getUser() != null ? order.getUser().getEmail() : null;
            if (ownerEmail != null && (requesterEmail == null || !ownerEmail.equalsIgnoreCase(requesterEmail))) {
                throw new ForbiddenException("Bạn không có quyền xem đơn hàng này.");
            }
        }
        return order;
    }

    @Override
    public int expireUnpaidOrders() {
        List<Order> expiredOrders = orderRepository.findByStatusAndExpiresAtBefore(
                OrderStatus.PENDING.name(), LocalDateTime.now());
        int count = 0;
        for (Order order : expiredOrders) {
            try {
                updateOrderStatus(order.getId(), OrderStatus.EXPIRED.name());
                count++;
                log.info("[Order Expiration] Đã tự động hết hạn đơn #{} và hoàn tồn kho.", order.getId());
            } catch (Exception ex) {
                log.warn("[Order Expiration] Bỏ qua đơn #{}: {}", order.getId(), ex.getMessage());
            }
        }
        return count;
    }

    private Set<Long> compensateOrderResources(Order order, String newItemStatus) {
        Set<Long> affectedProductIds = new HashSet<>();
        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        if (items != null && !items.isEmpty()) {
            List<OrderItem> sortedItems = new ArrayList<>(items);
            sortedItems.sort(Comparator.comparing(
                    i -> i.getProduct() != null && i.getProduct().getId() != null ? i.getProduct().getId() : 0L));

            for (OrderItem item : sortedItems) {
                if (item.getProduct() != null && item.getProduct().getId() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    Long productId = item.getProduct().getId();
                    productRepository.findByIdForUpdate(productId).ifPresent(product -> {
                        int currentStock = product.getStock() != null ? product.getStock() : 0;
                        product.setStock(currentStock + item.getQuantity());
                        productRepository.save(product);
                        affectedProductIds.add(productId);
                    });
                }
                item.setStatus(newItemStatus);
                orderItemRepository.save(item);
            }
        }

        if (couponOperations != null) {
            couponOperations.restoreCouponIfPresent(order.getCouponCode(), LocalDateTime.now());
        }
        return affectedProductIds;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("không tìm thấy đơn hàng"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserEmail(String email) {
        return orderRepository.findByUser_Email(email);
    }

    // ---- Các phương thức ủy quyền giữ tương thích ngược (ưu tiên dùng PaymentWebhookOperations / OrderItemOperations) ----

    public String computeWebhookSignature(Long orderId, String transactionId, String status) {
        return signatureVerifier.computeSignature(orderId, transactionId, status);
    }

    public boolean verifyWebhookSignature(PaymentWebhookDTO webhook, String providedSignature) {
        return signatureVerifier.verify(webhook, providedSignature);
    }

    /** @deprecated Sử dụng {@link #getOrderById(Long)} */
    @Deprecated
    public Order ReadOne(Long id) {
        return getOrderById(id);
    }

    /** @deprecated Sử dụng {@link OrderItemOperations#getAllOrderItems()} */
    @Deprecated
    public List<OrderItem> getAllOrderItems() {
        return orderItemOperations.getAllOrderItems();
    }

    /** @deprecated Sử dụng {@link OrderItemOperations#getAllOrderItems()} */
    @Deprecated
    public List<OrderItem> ReadAll() {
        return getAllOrderItems();
    }

    /** @deprecated Sử dụng {@link OrderItemOperations#getOrderItemsByUserEmail(String)} */
    @Deprecated
    public List<OrderItem> getOrderItemsByUserEmail(String email) {
        return orderItemOperations.getOrderItemsByUserEmail(email);
    }

    /** @deprecated Sử dụng {@link OrderItemOperations#updateOrderItemStatus(Long, String)} */
    @Deprecated
    public OrderItem updateOrderItemStatus(Long itemId, String status) {
        return orderItemOperations.updateOrderItemStatus(itemId, status);
    }
}
