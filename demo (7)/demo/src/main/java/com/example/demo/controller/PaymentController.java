package com.example.demo.controller;

import com.example.demo.dto.OrderDTO.PaymentWebhookDTO;
import com.example.demo.dto.ResponseDTO;
import com.example.demo.entity.Order;
import com.example.demo.service.contract.PaymentWebhookOperations;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment Webhook & Sandbox Simulator:
 * Chỉ phụ thuộc vào {@link PaymentWebhookOperations} thay vì toàn bộ OrderService (tuân thủ ISP & DIP).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentWebhookOperations paymentWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<ResponseDTO<Order>> handleWebhook(
            @Valid @RequestBody PaymentWebhookDTO payload,
            @RequestHeader(value = "X-Payment-Signature", required = false) String signature) {
        Order updatedOrder = paymentWebhookService.handlePaymentWebhook(payload, signature);
        return ResponseEntity.ok(ResponseDTO.success(updatedOrder));
    }

    @PostMapping("/sandbox/pay/{orderId}")
    public ResponseEntity<ResponseDTO<Order>> simulateSandboxPayment(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "SUCCESS") String status) {
        Order updated = paymentWebhookService.simulateSandboxPayment(orderId, status);
        return ResponseEntity.ok(ResponseDTO.success(updated));
    }
}
