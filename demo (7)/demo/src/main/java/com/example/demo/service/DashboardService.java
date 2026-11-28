package com.example.demo.service;

import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.config.CacheConfig;
import com.example.demo.dto.DailyRevenueDTO;
import com.example.demo.dto.DashboardDTO;
import com.example.demo.entity.Order;
import com.example.demo.service.contract.DashboardOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService implements DashboardOperations {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CACHE_DASHBOARD_STATS, key = "'overview'")
    public DashboardDTO getDashboardStats() {
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Long newOrders = orderRepository.count();
        Long totalProducts = productRepository.countByDeletedFalse();
        Long totalCustomers = userRepository.count();

        List<DailyRevenueDTO> dailyRevenue = orderRepository.getDailyRevenue()
                .stream()
                .map(DailyRevenueDTO::from)
                .toList();

        List<Order> recentOrders = orderRepository.findTop10ByOrderByOrderDateDesc();

        return DashboardDTO.builder()
                .totalRevenue(totalRevenue)
                .newOrders(newOrders)
                .totalProducts(totalProducts)
                .totalCustomers(totalCustomers)
                .dailyRevenue(dailyRevenue)
                .recentOrders(recentOrders)
                .build();
    }

    @CacheEvict(value = CacheConfig.CACHE_DASHBOARD_STATS, allEntries = true)
    public void evictDashboardStatsCache() {
        // Được gọi khi cần làm mới thống kê dashboard chủ động
    }
}
