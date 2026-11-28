package com.example.demo.service.contract;

/**
 * Interface chuyên biệt cho việc gửi thông báo OTP (ISP & DIP).
 * Dễ dàng mở rộng sang SMS / Zalo ZNS mà không ảnh hưởng tới thông báo hóa đơn đơn hàng.
 */
public interface OtpNotificationSender {

    void sendOtpEmail(String recipient, String otp);
}
