package com.example.demo.controller;

import com.example.demo.service.contract.SystemHealthOperations;
import com.example.demo.service.contract.SystemHealthOperations.SystemHealthReport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class HealthCheckController {

    private final SystemHealthOperations systemHealthService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        SystemHealthReport report = systemHealthService.checkHealth();
        return report.up()
                ? ResponseEntity.ok(report.payload())
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(report.payload());
    }
}
