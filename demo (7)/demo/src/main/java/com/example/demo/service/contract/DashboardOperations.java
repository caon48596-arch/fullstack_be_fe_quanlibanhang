package com.example.demo.service.contract;

import com.example.demo.dto.DashboardDTO;

/**
 * Hợp đồng thống kê số liệu Dashboard (DIP).
 */
public interface DashboardOperations {

    DashboardDTO getDashboardStats();

    void evictDashboardStatsCache();
}
