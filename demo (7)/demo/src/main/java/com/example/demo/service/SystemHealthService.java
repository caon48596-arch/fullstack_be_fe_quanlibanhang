package com.example.demo.service;

import com.example.demo.service.contract.SystemHealthOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tách logic kiểm tra kết nối hạ tầng Database ra khỏi HealthCheckController (SRP & DIP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemHealthService implements SystemHealthOperations {

    private final DataSource dataSource;

    @Override
    public SystemHealthReport checkHealth() {
        long startNs = System.nanoTime();
        boolean dbUp = false;

        try (Connection connection = dataSource.getConnection()) {
            dbUp = connection.isValid(2);
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
        }

        long latencyMs = (System.nanoTime() - startNs) / 1_000_000;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", dbUp ? "UP" : "DOWN");
        payload.put("timestamp", LocalDateTime.now());
        payload.put("service", "qlbanhang-api");
        payload.put("database", dbUp ? "Connected" : "Disconnected");
        payload.put("dbLatencyMs", latencyMs);

        return new SystemHealthReport(dbUp, payload);
    }
}
