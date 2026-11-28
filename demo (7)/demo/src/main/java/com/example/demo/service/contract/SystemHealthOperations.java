package com.example.demo.service.contract;

import java.util.Map;

/**
 * Hợp đồng kiểm tra sức khỏe hệ thống (SRP & DIP).
 */
public interface SystemHealthOperations {

    record SystemHealthReport(boolean up, Map<String, Object> payload) {}

    SystemHealthReport checkHealth();
}
