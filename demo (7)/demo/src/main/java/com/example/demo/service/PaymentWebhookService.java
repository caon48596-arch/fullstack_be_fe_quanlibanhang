package com.example.demo.service;

import com.example.demo.Repository.OrderRepository;
import com.example.demo.dto.OrderDTO.PaymentWebhookDTO;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderStatus;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.contract.OrderOperations;
import com.example.demo.service.contract.PaymentWebhookOperations;
import com.example.demo.service.payment.HmacSha256PaymentSignatureVerifier;
import com.example.demo.service.payment.PaymentSignatureVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tách riêng xử lý Payment Webhook, Idempotency giao dịch & Sandbox khỏi OrderService (SRP, OCP, ISP, DIP).
 */
@Service
@Transactional
public class PaymentWebhookService implements PaymentWebhookOperations {

    private final OrderRepository orderRepository;
    private final OrderOperations orderOperations;
    private final List<PaymentSignatureVerifier> signatureVerifiers;

    @Autowired
    public PaymentWebhookService(
            OrderRepository orderRepository,
            OrderOperations orderOperations,
            List<PaymentSignatureVerifier> signatureVerifiers) {
        this.orderRepository = orderRepository;
        this.orderOperations = orderOperations;
        this.signatureVerifiers = (signatureVerifiers != null && !signatureVerifiers.isEmpty())
                ? List.copyOf(signatureVerifiers)
                : List.of(new HmacSha256PaymentSignatureVerifier());
    }

    private PaymentSignatureVerifier resolveVerifier(String provider) {
        return signatureVerifiers.stream()
                .filter(v -> v.supports(provider))
                .findFirst()
                .orElseGet(HmacSha256PaymentSignatureVerifier::new);
    }

    @Override
    public Order handlePaymentWebhook(PaymentWebhookDTO webhook, String signatureHeader) {
        if (!verifyWebhookSignature(webhook, signatureHeader)) {
            throw new UnauthorizedException("Chữ ký Payment Webhook không hợp lệ.");
        }

        Optional<Order> alreadyProcessed = orderRepository.findByPaymentTransactionId(webhook.getTransactionId());
        if (alreadyProcessed.isPresent()) {
            return alreadyProcessed.get();
        }

        Order order = orderRepository.findByIdForUpdate(webhook.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng #" + webhook.getOrderId()));

        String paymentResult = webhook.getStatus().trim().toUpperCase();
        order.setPaymentTransactionId(webhook.getTransactionId().trim());

        if ("SUCCESS".equals(paymentResult)) {
            if (webhook.getAmount() != null && order.getTotalAmount() != null
                    && webhook.getAmount().compareTo(order.getTotalAmount()) != 0) {
                throw new BusinessException("Số tiền thanh toán không khớp với tổng giá trị đơn hàng.");
            }
            orderRepository.save(order);
            return orderOperations.updateOrderStatus(order.getId(), OrderStatus.PAID.name());
        } else if ("FAILED".equals(paymentResult)) {
            order.setPaymentStatus("FAILED");
            orderRepository.save(order);
            return orderOperations.updateOrderStatus(order.getId(), OrderStatus.CANCELLED.name());
        } else {
            throw new BusinessException("Trạng thái thanh toán webhook không hợp lệ: " + webhook.getStatus());
        }
    }

    @Override
    public Order simulateSandboxPayment(Long orderId, String status) {
        Order order = orderOperations.getOrderById(orderId);
        PaymentWebhookDTO dto = new PaymentWebhookDTO();
        dto.setOrderId(orderId);
        dto.setTransactionId("SANDBOX-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        dto.setStatus(status);
        dto.setAmount(order.getTotalAmount());
        dto.setProvider("SANDBOX");

        String signature = computeWebhookSignature(dto.getOrderId(), dto.getTransactionId(), dto.getStatus());
        return handlePaymentWebhook(dto, signature);
    }

    @Override
    public String computeWebhookSignature(Long orderId, String transactionId, String status) {
        return resolveVerifier("SANDBOX").computeSignature(orderId, transactionId, status);
    }

    @Override
    public boolean verifyWebhookSignature(PaymentWebhookDTO webhook, String providedSignature) {
        if (webhook == null) {
            return false;
        }
        return resolveVerifier(webhook.getProvider()).verify(webhook, providedSignature);
    }
}
