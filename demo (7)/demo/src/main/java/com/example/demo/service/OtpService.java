package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.service.contract.OtpOperations;
import com.example.demo.service.otp.CaffeineOtpStore;
import com.example.demo.service.otp.OtpStore;
import com.example.demo.service.otp.RedisOtpStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Nghiệp vụ sinh và kiểm chứng mã OTP, tách biệt hoàn toàn với cơ chế lưu trữ qua {@link OtpStore} (SRP, LSP, DIP).
 */
@Service
@Slf4j
public class OtpService implements OtpOperations {

    private static final long OTP_TTL_SECONDS = 300L;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom secureRandom = new SecureRandom();
    private final OtpStore fallbackStore = new CaffeineOtpStore(OTP_TTL_SECONDS, 50_000L);

    private OtpStore redisStore;
    private String cacheType = "redis";

    public OtpService() {
    }

    @Autowired
    public OtpService(
            @Autowired(required = false) StringRedisTemplate stringRedisTemplate,
            @Value("${app.cache.type:redis}") String cacheType) {
        setStringRedisTemplate(stringRedisTemplate);
        this.cacheType = cacheType;
    }

    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.redisStore = stringRedisTemplate != null ? new RedisOtpStore(stringRedisTemplate) : null;
    }

    public void setCacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    private boolean shouldUseRedis() {
        return redisStore != null && "redis".equalsIgnoreCase(cacheType);
    }

    private String normalizeKey(String identifier) {
        return identifier.trim().toLowerCase();
    }

    @Override
    public String generateOtp(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new BusinessException("Email hoặc số điện thoại không được để trống.");
        }

        String key = normalizeKey(identifier);
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        if (shouldUseRedis()) {
            try {
                redisStore.save(key, otp, OTP_TTL_SECONDS);
                fallbackStore.invalidate(key);
                return otp;
            } catch (DataAccessException ex) {
                log.warn("[OTP Redis Fallback] Không thể lưu OTP lên Redis cho '{}' (fallback Caffeine): {}",
                        key, ex.getMessage());
            }
        }

        fallbackStore.save(key, otp, OTP_TTL_SECONDS);
        return otp;
    }

    @Override
    public void verifyOtp(String identifier, String otp) {
        if (identifier == null || identifier.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            throw new BusinessException("Thiếu thông tin xác thực OTP.");
        }

        String key = normalizeKey(identifier);
        String submittedOtp = otp.trim();

        if (shouldUseRedis()) {
            try {
                if (redisStore.verifyIfPresent(key, submittedOtp, MAX_ATTEMPTS)) {
                    fallbackStore.invalidate(key);
                    return;
                }
            } catch (DataAccessException ex) {
                log.warn("[OTP Redis Fallback] Không thể xác thực OTP từ Redis cho '{}' (fallback Caffeine): {}",
                        key, ex.getMessage());
            }
        }

        if (!fallbackStore.verifyIfPresent(key, submittedOtp, MAX_ATTEMPTS)) {
            throw new BusinessException("Mã OTP không tồn tại hoặc đã hết hạn.");
        }
    }
}
