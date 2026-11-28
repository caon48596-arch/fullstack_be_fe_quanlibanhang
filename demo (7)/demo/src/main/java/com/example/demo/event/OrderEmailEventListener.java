package com.example.demo.event;

import com.example.demo.service.contract.OrderNotificationSender;
import com.example.demo.service.contract.OutboxOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class OrderEmailEventListener {

    private final OrderNotificationSender orderNotificationSender;
    private final OutboxOperations outboxOperations;

    @Autowired
    public OrderEmailEventListener(
            OrderNotificationSender orderNotificationSender,
            @Autowired(required = false) OutboxOperations outboxOperations) {
        this.orderNotificationSender = orderNotificationSender;
        this.outboxOperations = outboxOperations;
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        if (event == null || event.customerEmail() == null || event.customerEmail().isBlank()) {
            return;
        }
        log.info("[Async Outbox] Xử lý hóa đơn #HD-{} tới {} trên luồng {}",
                event.orderId(), event.customerEmail(), Thread.currentThread().getName());

        if (event.outboxEventId() != null && outboxOperations != null) {
            outboxOperations.processEventById(event.outboxEventId());
        } else {
            orderNotificationSender.sendOrderInvoiceEmail(event);
        }
    }
}
