package com.example.demo.service.notification;

import com.example.demo.event.OrderPlacedEvent;

/**
 * Tách riêng trách nhiệm dựng nội dung/template Email khỏi hạ tầng gửi SMTP (SRP & OCP).
 * Dễ dàng thay thế bằng Thymeleaf / FreeMarker / Database Template trong tương lai.
 */
public interface EmailTemplateRenderer {

    record RenderedEmail(String subject, String htmlContent, String label) {}

    RenderedEmail renderOtpEmail(String otp);

    RenderedEmail renderOrderInvoiceEmail(OrderPlacedEvent event);
}
