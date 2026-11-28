package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Bảng Transactional Outbox: lưu sự kiện domain trong cùng transaction DB với đơn hàng,
 * giúp chống mất sự kiện (Email hóa đơn, thông báo...) nếu server crash sau khi commit.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status_next_retry", columnList = "status, next_retry_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String EVENT_ORDER_PLACED = "ORDER_PLACED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private int maxRetries = 5;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
