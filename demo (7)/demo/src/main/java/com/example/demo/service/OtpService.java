package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // Lưu OTP trong bộ nhớ (key: email/SĐT), hiệu lực 5 phút, dùng 1 lần, tối đa 5 lần nhập sai
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateOtp(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new RuntimeException("Email hoặc số điện thoại không được để trống.");
        }

        String otp = String.format("%06d", random.nextInt(1000000));
        OtpData data = new OtpData(otp, LocalDateTime.now().plusSeconds(300));
        otpStorage.put(identifier.trim(), data);

        return otp;
    }

    public void verifyOtp(String identifier, String otp) {
        if (identifier == null || identifier.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            throw new RuntimeException("Thiếu thông tin xác thực OTP.");
        }

        OtpData data = otpStorage.get(identifier.trim());

        if (data == null) {
            throw new RuntimeException("Mã OTP không tồn tại hoặc đã hết hạn.");
        }

        if (LocalDateTime.now().isAfter(data.expirationTime)) {
            otpStorage.remove(identifier);
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng nhận mã mới.");
        }

        if (data.otp.equals(otp.trim())) {
            otpStorage.remove(identifier);
            return;
        }

        data.attempts++;
        if (data.attempts >= 5) {
            otpStorage.remove(identifier);
            throw new RuntimeException("Bạn đã nhập sai mã OTP quá 5 lần. Vui lòng nhận mã mới.");
        }

        throw new RuntimeException("Mã OTP không chính xác. Bạn còn " + (5 - data.attempts) + " lần thử.");
    }

    private static class OtpData {
        String otp;
        LocalDateTime expirationTime;
        int attempts = 0;

        OtpData(String otp, LocalDateTime expirationTime) {
            this.otp = otp;
            this.expirationTime = expirationTime;
        }
    }
}
