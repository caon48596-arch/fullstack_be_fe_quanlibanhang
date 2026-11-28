package com.example.demo.controller;

import com.example.demo.dto.DashboardDTO;
import com.example.demo.dto.ResponseDTO;
import com.example.demo.service.contract.DashboardOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardOperations dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ResponseDTO<DashboardDTO>> getDashboardStats() {
        return ResponseEntity.ok(ResponseDTO.success(dashboardService.getDashboardStats()));
    }
}
