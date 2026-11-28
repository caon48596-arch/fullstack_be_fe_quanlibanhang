package com.example.demo.service;

import com.example.demo.event.OrderPlacedEvent;
import com.example.demo.service.contract.OrderNotificationSender;
import com.example.demo.service.contract.OtpNotificationSender;
import com.example.demo.service.notification.EmailTemplateRenderer;
import com.example.demo.service.notification.EmailTemplateRenderer.RenderedEmail;
import com.example.demo.service.notification.HtmlEmailTemplateRenderer;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Phụ trách hạ tầng gửi Email qua SMTP, ủy quyền việc dựng HTML cho {@link EmailTemplateRenderer}
 * và cài đặt 2 interface chuyên biệt {@link OtpNotificationSender}, {@link OrderNotificationSender} (SRP, OCP, ISP, DIP).
 */
@Service
@Slf4j
public class EmailService implements OtpNotificationSender, OrderNotificationSender {

    private JavaMailSender mailSender;
    private EmailTemplateRenderer templateRenderer;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService() {
        this.templateRenderer = new HtmlEmailTemplateRenderer();
    }

    @Autowired
    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Autowired(required = false) EmailTemplateRenderer templateRenderer,
            @Value("${spring.mail.username:}") String fromEmail) {
        this.mailSender = mailSender;
        this.templateRenderer = templateRenderer != null ? templateRenderer : new HtmlEmailTemplateRenderer();
        this.fromEmail = fromEmail;
    }

    private EmailTemplateRenderer renderer() {
        if (templateRenderer == null) {
            templateRenderer = new HtmlEmailTemplateRenderer();
        }
        return templateRenderer;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendOtpEmail(String toEmail, String otp) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            return;
        }
        RenderedEmail rendered = renderer().renderOtpEmail(otp);
        sendHtmlEmail(toEmail.trim(), rendered.subject(), rendered.htmlContent(), rendered.label());
    }

    @Override
    public void sendOrderInvoiceEmail(OrderPlacedEvent event) {
        if (event == null || event.customerEmail() == null || event.customerEmail().trim().isEmpty()) {
            return;
        }
        RenderedEmail rendered = renderer().renderOrderInvoiceEmail(event);
        sendHtmlEmail(event.customerEmail().trim(), rendered.subject(), rendered.htmlContent(), rendered.label());
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent, String label) {
        try {
            if (mailSender != null && fromEmail != null && !fromEmail.isBlank()) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail, "Hệ Thống Quản Lý Bán Hàng IPWORLD");
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("[Async Email] Đã gửi {} tới {} (thread={})", label, toEmail, Thread.currentThread().getName());
            } else {
                log.info("[Async Email Fallback] Chưa cấu hình SMTP -> giả lập gửi {} tới {}", label, toEmail);
            }
        } catch (Exception e) {
            log.warn("[Async Email] Lỗi gửi {} tới {}: {}", label, toEmail, e.getMessage());
        }
    }
}
