package com.example.demo.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * State Machine kiểm soát luồng chuyển trạng thái hợp lệ cho đơn hàng E-commerce.
 * Ngăn chặn các chuyển đổi sai nghiệp vụ (VD: CANCELLED -> PAID, COMPLETED -> PENDING).
 */
public enum OrderStatus {
    PENDING,
    PAID,
    CONFIRMED,
    APPROVED,
    SHIPPING,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    EXPIRED;

    public Set<OrderStatus> allowedNextStates() {
        return switch (this) {
            case PENDING -> EnumSet.of(PAID, CONFIRMED, APPROVED, SHIPPING, SHIPPED, CANCELLED, EXPIRED);
            case PAID -> EnumSet.of(CONFIRMED, APPROVED, SHIPPING, SHIPPED, COMPLETED, CANCELLED);
            case CONFIRMED, APPROVED -> EnumSet.of(SHIPPING, SHIPPED, COMPLETED, CANCELLED);
            case SHIPPING, SHIPPED -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED, EXPIRED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus target) {
        return target != null && allowedNextStates().contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == EXPIRED;
    }

    public boolean requiresStockCompensation() {
        return this == CANCELLED || this == EXPIRED;
    }

    public static OrderStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không được để trống");
        }
        try {
            return OrderStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + value);
        }
    }
}
