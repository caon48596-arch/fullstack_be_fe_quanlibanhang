package com.example.demo.service.otp;

import com.example.demo.exception.BusinessException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;

/**
 * Lưu trữ OTP phân tán trên Redis với TTL và atomic HINCRBY đếm số lần thử sai (SRP & LSP).
 */
public class RedisOtpStore implements OtpStore {

    private static final String REDIS_KEY_PREFIX = "ipworld:otp:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisOtpStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void save(String normalizedKey, String otp, long ttlSeconds) {
        String redisKey = REDIS_KEY_PREFIX + normalizedKey;
        HashOperations<String, Object, Object> hashOps = stringRedisTemplate.opsForHash();
        hashOps.putAll(redisKey, Map.of("otp", otp, "attempts", "0"));
        stringRedisTemplate.expire(redisKey, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean verifyIfPresent(String normalizedKey, String submittedOtp, int maxAttempts) {
        String redisKey = REDIS_KEY_PREFIX + normalizedKey;
        HashOperations<String, Object, Object> hashOps = stringRedisTemplate.opsForHash();
        Object storedOtpObj = hashOps.get(redisKey, "otp");
        if (storedOtpObj == null) {
            return false;
        }

        String storedOtp = storedOtpObj.toString();
        boolean matched = MessageDigest.isEqual(
                storedOtp.getBytes(StandardCharsets.UTF_8),
                submittedOtp.getBytes(StandardCharsets.UTF_8));

        if (matched) {
            stringRedisTemplate.delete(redisKey);
            return true;
        }

        Long updatedAttempts = hashOps.increment(redisKey, "attempts", 1L);
        long attempts = updatedAttempts != null ? updatedAttempts : maxAttempts;
        if (attempts >= maxAttempts) {
            stringRedisTemplate.delete(redisKey);
            throw new BusinessException("Bạn đã nhập sai mã OTP quá " + maxAttempts + " lần. Vui lòng nhận mã mới.");
        }

        throw new BusinessException("Mã OTP không chính xác. Bạn còn " + (maxAttempts - attempts) + " lần thử.");
    }

    @Override
    public void invalidate(String normalizedKey) {
        stringRedisTemplate.delete(REDIS_KEY_PREFIX + normalizedKey);
    }
}
