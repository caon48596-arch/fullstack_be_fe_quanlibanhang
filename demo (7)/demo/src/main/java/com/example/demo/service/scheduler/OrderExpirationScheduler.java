package com.example.demo.service.scheduler;

import com.example.demo.service.contract.OrderOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tách tác vụ lập lịch định kỳ hết hạn đơn hàng ra khỏi OrderService (SRP & DIP).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationScheduler {

    private final OrderOperations orderOperations;

    @Scheduled(fixedDelayString = "${app.orders.expiration-check-ms:60000}")
    public void scheduledExpireUnpaidOrders() {
        int expired = orderOperations.expireUnpaidOrders();
        if (expired > 0) {
            log.info("[Order Expiration] Scheduled job đã tự động hết hạn và hoàn kho cho {} đơn hàng.", expired);
        }
    }
}
