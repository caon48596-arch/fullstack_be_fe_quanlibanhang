package com.example.demo.service.outbox;

import com.example.demo.entity.OutboxEvent;

/**
 * Strategy xử lý từng loại sự kiện trong bảng Transactional Outbox (tuân thủ OCP).
 * Khi thêm loại sự kiện mới (VD: ORDER_CANCELLED, PAYMENT_CONFIRMED, INVENTORY_SYNC),
 * chỉ cần thêm một @Component implement interface này mà không phải sửa OutboxService.
 */
public interface OutboxEventHandler {

    String supportedEventType();

    void handle(OutboxEvent outboxEvent) throws Exception;
}
