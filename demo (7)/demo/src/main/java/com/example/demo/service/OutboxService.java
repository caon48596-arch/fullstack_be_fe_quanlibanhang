package com.example.demo.service;

import com.example.demo.Repository.OutboxEventRepository;
import com.example.demo.entity.OutboxEvent;
import com.example.demo.event.OrderPlacedEvent;
import com.example.demo.service.contract.OutboxOperations;
import com.example.demo.service.outbox.OutboxEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Triển khai Transactional Outbox Pattern theo nguyên lý Open/Closed (OCP) & Dependency Inversion (DIP):
 * điều phối sự kiện thông qua registry {@link OutboxEventHandler} thay vì if-else cứng theo loại sự kiện,
 * và tách tác vụ lập lịch định kỳ sang {@link com.example.demo.service.scheduler.OutboxRelayScheduler} (SRP).
 */
@Service
@Slf4j
public class OutboxService implements OutboxOperations {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, OutboxEventHandler> handlerRegistry;

    @Autowired
    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) List<OutboxEventHandler> handlers) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.handlerRegistry = (handlers != null && !handlers.isEmpty())
                ? handlers.stream().collect(Collectors.toUnmodifiableMap(
                        OutboxEventHandler::supportedEventType,
                        Function.identity(),
                        (existing, replacement) -> replacement))
                : Collections.emptyMap();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent recordOrderPlacedEvent(OrderPlacedEvent event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            LocalDateTime now = LocalDateTime.now();
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(event.orderId())
                    .eventType(OutboxEvent.EVENT_ORDER_PLACED)
                    .payload(payloadJson)
                    .status(OutboxEvent.STATUS_PENDING)
                    .retryCount(0)
                    .maxRetries(5)
                    .nextRetryAt(now)
                    .createdAt(now)
                    .build();
            return outboxEventRepository.save(outbox);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tuần tự hóa sự kiện Outbox cho đơn hàng #" + event.orderId(), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processEventById(Long outboxEventId) {
        if (outboxEventId == null) {
            return false;
        }
        OutboxEvent event = outboxEventRepository.findById(outboxEventId).orElse(null);
        if (event == null || !OutboxEvent.STATUS_PENDING.equals(event.getStatus())) {
            return false;
        }
        return dispatchAndUpdate(event);
    }

    @Override
    @Transactional
    public int relayPendingEvents(int batchSize) {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        OutboxEvent.STATUS_PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, Math.max(1, batchSize)));

        int processedCount = 0;
        for (OutboxEvent event : pendingEvents) {
            if (dispatchAndUpdate(event)) {
                processedCount++;
            }
        }
        return processedCount;
    }

    private boolean dispatchAndUpdate(OutboxEvent outbox) {
        try {
            OutboxEventHandler handler = handlerRegistry.get(outbox.getEventType());
            if (handler != null) {
                handler.handle(outbox);
            } else {
                log.warn("[Outbox] Bỏ qua loại sự kiện chưa có OutboxEventHandler đăng ký: {}", outbox.getEventType());
            }

            outbox.setStatus(OutboxEvent.STATUS_PROCESSED);
            outbox.setProcessedAt(LocalDateTime.now());
            outbox.setLastError(null);
            outboxEventRepository.save(outbox);
            return true;
        } catch (Exception ex) {
            int nextRetry = outbox.getRetryCount() + 1;
            outbox.setRetryCount(nextRetry);
            outbox.setLastError(truncate(ex.getMessage(), 950));

            if (nextRetry >= outbox.getMaxRetries()) {
                outbox.setStatus(OutboxEvent.STATUS_FAILED);
                log.error("[Outbox] Sự kiện #{} ({}) thất bại sau {} lần thử: {}",
                        outbox.getId(), outbox.getEventType(), nextRetry, ex.getMessage());
            } else {
                long delaySeconds = (long) Math.pow(2, nextRetry - 1) * 30L;
                outbox.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
                log.warn("[Outbox] Lỗi xử lý sự kiện #{} (lần {}/{}), sẽ thử lại sau {}s: {}",
                        outbox.getId(), nextRetry, outbox.getMaxRetries(), delaySeconds, ex.getMessage());
            }
            outboxEventRepository.save(outbox);
            return false;
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
