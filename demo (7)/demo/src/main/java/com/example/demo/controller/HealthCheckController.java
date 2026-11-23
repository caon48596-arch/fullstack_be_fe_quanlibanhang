package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Slf4j
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        log.info("System health check API was called.");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "qlbanhang-api");
        response.put("database", "Connected");

        return ResponseEntity.ok(response);
    }
}