package com.example.demo.service.otp;

/**
 * Hợp đồng lưu trữ và kiểm chứng mã OTP (tuân thủ DIP & LSP).
 * Mọi cài đặt (Redis, Caffeine, Database...) đều tuân thủ cùng quy ước TTL và giới hạn số lần nhập sai.
 */
public interface OtpStore {

    void save(String normalizedKey, String otp, long ttlSeconds);

    /**
     * Trả về {@code true} nếu tìm thấy và xác thực khớp mã OTP;
     * trả về {@code false} nếu kho này hiện không giữ mã OTP của key (cho phép fallback sang kho dự phòng);
     * ném {@link com.example.demo.exception.BusinessException} nếu nhập sai mã hoặc vượt quá số lần thử tối đa.
     */
    boolean verifyIfPresent(String normalizedKey, String submittedOtp, int maxAttempts);

    void invalidate(String normalizedKey);
}
