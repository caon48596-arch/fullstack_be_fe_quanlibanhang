package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Repository.OrderRepository;
import com.example.demo.Repository.ProductRepository;
import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.DashboardDTO;
import com.example.demo.dto.ResponseDTO;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/stats")
    public ResponseEntity<ResponseDTO<DashboardDTO>> getDashboardStats() {
        Double totalRevenue = orderRepository.sumTotalRevenue();
        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }
        
        Long newOrders = orderRepository.count();
        Long totalProducts = productRepository.count();
        Long totalCustomers = userRepository.count();
        
        java.util.List<com.example.demo.dto.DailyRevenue> dailyRevenue = orderRepository.getDailyRevenue();
        java.util.List<com.example.demo.entity.Order> recentOrders = orderRepository.findTop10ByOrderByOrderDateDesc();

        DashboardDTO stats = DashboardDTO.builder()
                .totalRevenue(totalRevenue)
                .newOrders(newOrders)
                .totalProducts(totalProducts)
                .totalCustomers(totalCustomers)
                .dailyRevenue(dailyRevenue)
                .recentOrders(recentOrders)
                .build();

        return ResponseEntity.ok(ResponseDTO.success(stats));
    }
}
