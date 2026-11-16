package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDTO {
    private Double totalRevenue;
    private Long newOrders;
    private Long totalProducts;
    private Long totalCustomers;
    
    private java.util.List<DailyRevenue> dailyRevenue;
    private java.util.List<com.example.demo.entity.Order> recentOrders;
}
