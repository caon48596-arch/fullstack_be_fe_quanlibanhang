package com.example.demo.service.scheduler;

import com.example.demo.service.contract.OutboxOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tách worker định kỳ quét Outbox ra khỏi OutboxService (SRP & DIP).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxOperations outboxOperations;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:30000}")
    public void scheduledRelayPendingEvents() {
        int processed = outboxOperations.relayPendingEvents(20);
        if (processed > 0) {
            log.info("[Outbox Relay] Đã xử lý bù {} sự kiện tồn đọng.", processed);
        }
    }
}
