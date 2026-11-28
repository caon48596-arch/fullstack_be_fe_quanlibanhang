package com.example.demo.service.outbox;

import com.example.demo.entity.OutboxEvent;
import com.example.demo.event.OrderPlacedEvent;
import com.example.demo.service.contract.OrderNotificationSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Handler chuyên trách xử lý sự kiện ORDER_PLACED từ Transactional Outbox (SRP & OCP).
 */
@Component
@RequiredArgsConstructor
public class OrderPlacedOutboxHandler implements OutboxEventHandler {

    private final ObjectMapper objectMapper;
    private final OrderNotificationSender orderNotificationSender;

    @Override
    public String supportedEventType() {
        return OutboxEvent.EVENT_ORDER_PLACED;
    }

    @Override
    public void handle(OutboxEvent outboxEvent) throws Exception {
        OrderPlacedEvent payload = objectMapper.readValue(outboxEvent.getPayload(), OrderPlacedEvent.class);
        orderNotificationSender.sendOrderInvoiceEmail(payload);
    }
}
