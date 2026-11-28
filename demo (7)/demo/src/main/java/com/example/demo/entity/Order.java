package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
@Table(name = "Orders", indexes = {
    @Index(name = "idx_orders_user_date_status", columnList = "User_id, orderDate, status"),
    @Index(name = "idx_orders_order_date", columnList = "orderDate"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_status_expires_at", columnList = "status, expires_at")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime orderDate;

    @ManyToOne
    @JoinColumn(name = "User_id")
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    private String status = OrderStatus.PENDING.name();

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "payment_status", nullable = false, length = 32)
    private String paymentStatus = "UNPAID";

    @Column(name = "payment_transaction_id", unique = true, length = 128)
    private String paymentTransactionId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
