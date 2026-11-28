package com.example.demo.service.otp;

import com.example.demo.exception.BusinessException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Lưu trữ OTP trong bộ nhớ bằng Caffeine (dùng cho môi trường local/test hoặc khi Redis mất kết nối).
 */
public class CaffeineOtpStore implements OtpStore {

    private final Cache<String, OtpEntry> storage;

    public CaffeineOtpStore(long ttlSeconds, long maxSize) {
        this.storage = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .build();
    }

    @Override
    public void save(String normalizedKey, String otp, long ttlSeconds) {
        storage.put(normalizedKey, new OtpEntry(otp, LocalDateTime.now().plusSeconds(ttlSeconds)));
    }

    @Override
    public boolean verifyIfPresent(String normalizedKey, String submittedOtp, int maxAttempts) {
        OtpEntry data = storage.getIfPresent(normalizedKey);
        if (data == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(data.expirationTime)) {
            storage.invalidate(normalizedKey);
            throw new BusinessException("Mã OTP đã hết hạn. Vui lòng nhận mã mới.");
        }

        boolean matched = MessageDigest.isEqual(
                data.otp.getBytes(StandardCharsets.UTF_8),
                submittedOtp.getBytes(StandardCharsets.UTF_8));

        if (matched) {
            storage.invalidate(normalizedKey);
            return true;
        }

        data.attempts++;
        if (data.attempts >= maxAttempts) {
            storage.invalidate(normalizedKey);
            throw new BusinessException("Bạn đã nhập sai mã OTP quá " + maxAttempts + " lần. Vui lòng nhận mã mới.");
        }

        throw new BusinessException("Mã OTP không chính xác. Bạn còn " + (maxAttempts - data.attempts) + " lần thử.");
    }

    @Override
    public void invalidate(String normalizedKey) {
        storage.invalidate(normalizedKey);
    }

    private static final class OtpEntry {
        final String otp;
        final LocalDateTime expirationTime;
        int attempts = 0;

        OtpEntry(String otp, LocalDateTime expirationTime) {
            this.otp = otp;
            this.expirationTime = expirationTime;
        }
    }
}
