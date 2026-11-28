package com.example.demo.service.contract;

import com.example.demo.entity.OutboxEvent;
import com.example.demo.event.OrderPlacedEvent;

/**
 * Hợp đồng Transactional Outbox (DIP).
 */
public interface OutboxOperations {

    OutboxEvent recordOrderPlacedEvent(OrderPlacedEvent event);

    boolean processEventById(Long outboxEventId);

    int relayPendingEvents(int batchSize);
}
