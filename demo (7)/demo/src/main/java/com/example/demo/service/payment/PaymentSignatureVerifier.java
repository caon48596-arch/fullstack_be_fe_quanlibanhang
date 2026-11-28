package com.example.demo.service.payment;

import com.example.demo.dto.OrderDTO.PaymentWebhookDTO;

/**
 * Strategy xác thực chữ ký cổng thanh toán (tuân thủ OCP).
 * Dễ dàng bổ sung VNPay, MoMo, Stripe, ZaloPay bằng cách thêm bean mới implement interface này.
 */
public interface PaymentSignatureVerifier {

    boolean supports(String provider);

    String computeSignature(Long orderId, String transactionId, String status);

    boolean verify(PaymentWebhookDTO webhook, String providedSignature);
}
