package com.example.demo.Unit_test;

import com.example.demo.Repository.CouponRepository;
import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.OutboxEventRepository;
import com.example.demo.dto.OrderDTO.PaymentWebhookDTO;
import com.example.demo.entity.Coupon;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.entity.OutboxEvent;
import com.example.demo.service.CouponService;
import com.example.demo.service.OutboxService;
import com.example.demo.service.PaymentWebhookService;
import com.example.demo.service.contract.CouponOperations.AppliedCoupon;
import com.example.demo.service.contract.OrderOperations;
import com.example.demo.service.discount.CouponDiscountPolicy;
import com.example.demo.service.discount.FixedAmountCouponDiscountPolicy;
import com.example.demo.service.outbox.OutboxEventHandler;
import com.example.demo.service.payment.PaymentSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("SOLID Architecture Extensibility Unit Tests (OCP, DIP, ISP, SRP)")
class SolidArchitectureTest {

    @Test
    @DisplayName("OCP: Dễ dàng cắm thêm chính sách giảm giá theo % (Percentage Policy) mà không sửa CouponService")
    void testCouponService_OpenClosedForCustomPercentageDiscountPolicy() {
        CouponRepository couponRepository = mock(CouponRepository.class);

        // Giả lập nâng cấp trong tương lai: mã bắt đầu bằng "PCT_" sẽ giảm theo phần trăm (VD: 10%)
        CouponDiscountPolicy percentagePolicy = new CouponDiscountPolicy() {
            @Override
            public boolean supports(Coupon coupon) {
                return coupon != null && coupon.getCode() != null && coupon.getCode().startsWith("PCT_");
            }

            @Override
            public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderSubtotal) {
                return orderSubtotal.multiply(coupon.getDiscountAmount())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        };

        CouponService couponService = new CouponService(
                couponRepository,
                List.of(percentagePolicy, new FixedAmountCouponDiscountPolicy()));

        Coupon pctCoupon = new Coupon(1L, "PCT_10", new BigDecimal("10"), 5, true, LocalDateTime.now().plusDays(2));
        when(couponRepository.findByCodeForUpdate("PCT_10")).thenReturn(Optional.of(pctCoupon));
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        AppliedCoupon applied = couponService.applyCouponForOrder(
                "PCT_10",
                new BigDecimal("500000.00"),
                LocalDateTime.now());

        assertEquals("PCT_10", applied.code());
        assertEquals(0, new BigDecimal("50000.00").compareTo(applied.discountAmount()));
        assertEquals(0, new BigDecimal("450000.00").compareTo(applied.discountedTotal()));
        assertEquals(4, pctCoupon.getQuantity());
    }

    @Test
    @DisplayName("OCP: Đăng ký loại sự kiện Outbox mới (VD: ORDER_CANCELLED) qua OutboxEventHandler mà không sửa OutboxService")
    void testOutboxService_OpenClosedForNewEventHandlers() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        AtomicBoolean handled = new AtomicBoolean(false);

        OutboxEventHandler customCancelledHandler = new OutboxEventHandler() {
            @Override
            public String supportedEventType() {
                return "ORDER_CANCELLED";
            }

            @Override
            public void handle(OutboxEvent outboxEvent) {
                handled.set(true);
            }
        };

        OutboxService outboxService = new OutboxService(
                repository,
                new ObjectMapper(),
                List.of(customCancelledHandler));

        OutboxEvent pending = OutboxEvent.builder()
                .id(99L)
                .aggregateType("ORDER")
                .aggregateId(10L)
                .eventType("ORDER_CANCELLED")
                .payload("{\"orderId\":10}")
                .status(OutboxEvent.STATUS_PENDING)
                .retryCount(0)
                .maxRetries(3)
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findById(99L)).thenReturn(Optional.of(pending));
        when(repository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean ok = outboxService.processEventById(99L);

        assertTrue(ok);
        assertTrue(handled.get(), "Handler mới phải được tự động kích hoạt theo eventType");
        assertEquals(OutboxEvent.STATUS_PROCESSED, pending.getStatus());
    }

    @Test
    @DisplayName("OCP & DIP: Tích hợp cổng thanh toán mới (VD: STRIPE) qua PaymentSignatureVerifier mà không sửa PaymentWebhookService")
    void testPaymentWebhookService_SupportsPluggableProviderVerifier() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderOperations orderOperations = mock(OrderOperations.class);

        PaymentSignatureVerifier stripeVerifier = new PaymentSignatureVerifier() {
            @Override
            public boolean supports(String provider) {
                return "STRIPE".equalsIgnoreCase(provider);
            }

            @Override
            public String computeSignature(Long orderId, String transactionId, String status) {
                return "stripe_sig_" + orderId + "_" + transactionId;
            }

            @Override
            public boolean verify(PaymentWebhookDTO webhook, String providedSignature) {
                return computeSignature(webhook.getOrderId(), webhook.getTransactionId(), webhook.getStatus())
                        .equals(providedSignature);
            }
        };

        PaymentWebhookService webhookService = new PaymentWebhookService(
                orderRepository,
                orderOperations,
                List.of(stripeVerifier));

        Order existingOrder = new Order();
        existingOrder.setId(42L);
        existingOrder.setTotalAmount(new BigDecimal("1200000.00"));
        existingOrder.setStatus(OrderStatus.PENDING.name());

        Order paidOrder = new Order();
        paidOrder.setId(42L);
        paidOrder.setStatus(OrderStatus.PAID.name());
        paidOrder.setPaymentStatus("PAID");

        when(orderRepository.findByPaymentTransactionId("stripe_tx_01")).thenReturn(Optional.empty());
        when(orderRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(existingOrder));
        when(orderOperations.updateOrderStatus(42L, OrderStatus.PAID.name())).thenReturn(paidOrder);

        PaymentWebhookDTO dto = new PaymentWebhookDTO();
        dto.setOrderId(42L);
        dto.setTransactionId("stripe_tx_01");
        dto.setStatus("SUCCESS");
        dto.setAmount(new BigDecimal("1200000.00"));
        dto.setProvider("STRIPE");

        Order updated = webhookService.handlePaymentWebhook(dto, "stripe_sig_42_stripe_tx_01");

        assertEquals("PAID", updated.getStatus());
        verify(orderOperations, times(1)).updateOrderStatus(42L, OrderStatus.PAID.name());
    }
}
