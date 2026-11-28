package com.example.demo.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain Event phát sinh khi đơn hàng đã được lưu, gắn kèm outboxEventId
 * để fast-path AFTER_COMMIT xử lý ngay và đánh dấu PROCESSED trong bảng outbox_events.
 */
public record OrderPlacedEvent(
        Long outboxEventId,
        Long orderId,
        String customerEmail,
        String customerName,
        LocalDateTime orderDate,
        BigDecimal totalAmount,
        String couponCode,
        List<OrderItemSnapshot> items
) {
    public OrderPlacedEvent(
            Long orderId,
            String customerEmail,
            String customerName,
            LocalDateTime orderDate,
            BigDecimal totalAmount,
            String couponCode,
            List<OrderItemSnapshot> items) {
        this(null, orderId, customerEmail, customerName, orderDate, totalAmount, couponCode, items);
    }

    public record OrderItemSnapshot(
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {}
}
