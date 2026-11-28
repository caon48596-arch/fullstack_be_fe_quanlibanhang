package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDTO {
    private BigDecimal totalRevenue;
    private Long newOrders;
    private Long totalProducts;
    private Long totalCustomers;

    private List<DailyRevenueDTO> dailyRevenue;
    private List<com.example.demo.entity.Order> recentOrders;
}
