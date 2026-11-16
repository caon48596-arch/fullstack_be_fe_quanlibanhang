package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // Lưu trữ mã OTP trong bộ nhớ: Key = email/sđt, Value = OtpData
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String generateOtp(String identifier) {
        // Tạo mã OTP 6 chữ số
        String otp = String.format("%06d", random.nextInt(1000000));

        // Hạn sử dụng: 5 phút (300 giây) từ thời điểm tạo
        OtpData data = new OtpData(otp, LocalDateTime.now().plusSeconds(300));
        otpStorage.put(identifier, data);

        return otp;
    }

    public void verifyOtp(String identifier, String otp) {
        if (identifier == null || otp == null) {
            throw new RuntimeException("Thiếu thông tin xác thực OTP.");
        }

        OtpData data = otpStorage.get(identifier);

        if (data == null) {
            throw new RuntimeException("Mã OTP không tồn tại hoặc đã hết hạn.");
        }

        if (LocalDateTime.now().isAfter(data.expirationTime)) {
            otpStorage.remove(identifier); // Đã hết hạn
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng nhận mã mới.");
        }

        if (data.otp.equals(otp.trim())) {
            otpStorage.remove(identifier); // Dùng 1 lần
            return; // Thành công
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
