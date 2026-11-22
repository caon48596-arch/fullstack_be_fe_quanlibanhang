package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() throws Exception {
        // Cài đặt giá trị private fromEmail thông qua reflection
        Field field = EmailService.class.getDeclaredField("fromEmail");
        field.setAccessible(true);
        field.set(emailService, "noreply@example.com");
    }

    @Test
    @DisplayName("Gửi OTP thành công khi có JavaMailSender")
    void testSendOtpEmail_WithMailSender_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendOtpEmail("test@example.com", "123456"));

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Gửi OTP an toàn khi JavaMailSender là null (giả lập console)")
    void testSendOtpEmail_MailSenderNull_Fallback() throws Exception {
        Field field = EmailService.class.getDeclaredField("mailSender");
        field.setAccessible(true);
        field.set(emailService, null);

        assertDoesNotThrow(() -> emailService.sendOtpEmail("test@example.com", "123456"));
    }

    @Test
    @DisplayName("Xử lý an toàn khi gửi email bị exception")
    void testSendOtpEmail_ExceptionHandledSafely() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP Connection Error"));

        // EmailService catches Exception and prints log, should not crash
        assertDoesNotThrow(() -> emailService.sendOtpEmail("test@example.com", "123456"));
    }
}
