package com.example.demo.service.payment;

import com.example.demo.dto.OrderDTO.PaymentWebhookDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Triển khai xác thực chữ ký chuẩn HMAC-SHA256 (constant-time) cho Sandbox / Generic Webhook.
 */
@Component
@Order(100)
public class HmacSha256PaymentSignatureVerifier implements PaymentSignatureVerifier {

    private final String webhookSecret;

    public HmacSha256PaymentSignatureVerifier(
            @Value("${app.payment.webhook-secret:ipworld-dev-payment-webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public HmacSha256PaymentSignatureVerifier() {
        this("ipworld-dev-payment-webhook-secret");
    }

    @Override
    public boolean supports(String provider) {
        // Đóng vai trò fallback mặc định cho SANDBOX / VNPAY / MOMO nếu chưa có verifier chuyên biệt đứng trước
        return true;
    }

    @Override
    public String computeSignature(Long orderId, String transactionId, String status) {
        try {
            String canonical = orderId + ":" + transactionId + ":" + status.trim().toUpperCase();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tính chữ ký HMAC webhook", ex);
        }
    }

    @Override
    public boolean verify(PaymentWebhookDTO webhook, String providedSignature) {
        if (webhook == null || providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        String expected = computeSignature(webhook.getOrderId(), webhook.getTransactionId(), webhook.getStatus());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                providedSignature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
