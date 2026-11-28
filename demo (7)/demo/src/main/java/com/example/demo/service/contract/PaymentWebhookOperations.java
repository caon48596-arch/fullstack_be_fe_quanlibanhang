package com.example.demo.service.contract;

import com.example.demo.dto.OrderDTO.PaymentWebhookDTO;
import com.example.demo.entity.Order;

/**
 * Hợp đồng xử lý Cổng thanh toán & Payment Webhook (ISP & DIP).
 */
public interface PaymentWebhookOperations {

    Order handlePaymentWebhook(PaymentWebhookDTO webhook, String signatureHeader);

    Order simulateSandboxPayment(Long orderId, String status);

    String computeWebhookSignature(Long orderId, String transactionId, String status);

    boolean verifyWebhookSignature(PaymentWebhookDTO webhook, String providedSignature);
}
