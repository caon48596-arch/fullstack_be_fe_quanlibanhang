package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            if (toEmail != null && !toEmail.trim().isEmpty()) {
                if (mailSender != null) {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                    
                    // Cài đặt tên người gửi hiển thị
                    helper.setFrom(fromEmail, "Hệ Thống Quản Lý Bán Hàng");
                    helper.setTo(toEmail);
                    helper.setSubject("Mã Xác Thực OTP Của Bạn");
                    
                    String content = "<div style=\"font-family: Arial, sans-serif; padding: 20px; color: #333;\">"
                            + "<h2>Xác thực tài khoản</h2>"
                            + "<p>Mã xác thực OTP của bạn là:</p>"
                            + "<h1 style=\"color: #007bff; letter-spacing: 5px;\">" + otp + "</h1>"
                            + "<p>Mã này bao gồm 6 chữ số và có hiệu lực trong vòng 60 giây.</p>"
                            + "<p style=\"color: red;\">Tuyệt đối không chia sẻ mã này cho bất kỳ ai để bảo mật tài khoản.</p>"
                            + "</div>";
                            
                    helper.setText(content, true); // true = là HTML
                    
                    mailSender.send(message);
                    System.out.println("Đã gửi OTP đến email: " + toEmail);
                } else {
                    System.out.println("JavaMailSender không khả dụng. Giả lập gửi OTP: " + otp + " đến email " + toEmail);
                }
            }
        } catch (Exception e) {
            // Log lỗi nếu cấu hình email chưa đúng hoặc bị sai auth
            System.err.println("Lỗi khi gửi email thật, giả lập in ra console. OTP: " + otp + " đến " + toEmail);
            System.err.println("Chi tiết lỗi: " + e.getMessage());
        }
    }
}
