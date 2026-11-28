package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Concrete DTO cho doanh thu theo ngày — hỗ trợ serialize/deserialize JSON mượt mà trên Redis Cache.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyRevenueDTO implements DailyRevenue {
    private String date;
    private BigDecimal revenue;
    private Long orderCount;

    public static DailyRevenueDTO from(DailyRevenue projection) {
        return new DailyRevenueDTO(
                projection.getDate(),
                projection.getRevenue() != null ? projection.getRevenue() : BigDecimal.ZERO,
                projection.getOrderCount()
        );
    }
}
