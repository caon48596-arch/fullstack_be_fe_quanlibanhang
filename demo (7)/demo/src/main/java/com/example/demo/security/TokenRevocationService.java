package com.example.demo.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Quản lý JWT Blacklist (thu hồi Access Token ngay khi Logout) và Refresh Token Rotation
 * lưu tập trung trên Redis với TTL tự động hết hạn, fallback an toàn sang Caffeine ở môi trường local/test.
 */
@Service
@Slf4j
public class TokenRevocationService implements TokenRevocationOperations {

    private static final String BLACKLIST_PREFIX = "ipworld:jwt:blacklist:";
    private static final String REFRESH_PREFIX = "ipworld:jwt:refresh:";

    private StringRedisTemplate stringRedisTemplate;
    private String cacheType = "redis";

    private final Cache<String, Long> fallbackBlacklist = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(100_000)
            .build();

    private final Cache<String, String> fallbackRefreshTokens = Caffeine.newBuilder()
            .expireAfterWrite(7, TimeUnit.DAYS)
            .maximumSize(100_000)
            .build();

    @Autowired(required = false)
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Value("${app.cache.type:redis}")
    public void setCacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    private boolean shouldUseRedis() {
        return stringRedisTemplate != null && "redis".equalsIgnoreCase(cacheType);
    }

    public void revokeAccessToken(String jti, Date expiration) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        long remainingMs = expiration != null
                ? Math.max(1_000L, expiration.getTime() - System.currentTimeMillis())
                : TimeUnit.HOURS.toMillis(1);

        if (shouldUseRedis()) {
            try {
                stringRedisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + jti,
                        "1",
                        Duration.ofMillis(remainingMs));
                return;
            } catch (DataAccessException ex) {
                log.warn("[JWT Blacklist Fallback] Lỗi ghi Redis cho jti='{}': {}", jti, ex.getMessage());
            }
        }
        fallbackBlacklist.put(jti, System.currentTimeMillis() + remainingMs);
    }

    public boolean isTokenRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }

        if (shouldUseRedis()) {
            try {
                Boolean exists = stringRedisTemplate.hasKey(BLACKLIST_PREFIX + jti);
                if (Boolean.TRUE.equals(exists)) {
                    return true;
                }
            } catch (DataAccessException ex) {
                log.warn("[JWT Blacklist Fallback] Lỗi đọc Redis cho jti='{}': {}", jti, ex.getMessage());
            }
        }

        Long expiresAt = fallbackBlacklist.getIfPresent(jti);
        if (expiresAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiresAt) {
            fallbackBlacklist.invalidate(jti);
            return false;
        }
        return true;
    }

    public void registerRefreshToken(String jti, String subject, long ttlMillis) {
        if (jti == null || jti.isBlank() || subject == null || subject.isBlank()) {
            return;
        }
        long safeTtl = Math.max(60_000L, ttlMillis);

        if (shouldUseRedis()) {
            try {
                stringRedisTemplate.opsForValue().set(
                        REFRESH_PREFIX + jti,
                        subject,
                        Duration.ofMillis(safeTtl));
                return;
            } catch (DataAccessException ex) {
                log.warn("[Refresh Token Fallback] Lỗi ghi Redis cho jti='{}': {}", jti, ex.getMessage());
            }
        }
        fallbackRefreshTokens.put(jti, subject);
    }

    /**
     * Refresh Token Rotation: kiểm tra refresh token còn hợp lệ và xóa ngay sau 1 lần dùng
     * để chống replay attack nếu token cũ bị đánh cắp.
     */
    public boolean consumeRefreshToken(String jti, String expectedSubject) {
        if (jti == null || jti.isBlank() || expectedSubject == null) {
            return false;
        }

        if (shouldUseRedis()) {
            try {
                String redisKey = REFRESH_PREFIX + jti;
                String stored = stringRedisTemplate.opsForValue().get(redisKey);
                if (expectedSubject.equals(stored)) {
                    stringRedisTemplate.delete(redisKey);
                    return true;
                }
                if (stored != null) {
                    return false;
                }
            } catch (DataAccessException ex) {
                log.warn("[Refresh Token Fallback] Lỗi đọc Redis cho jti='{}': {}", jti, ex.getMessage());
            }
        }

        String storedLocal = fallbackRefreshTokens.getIfPresent(jti);
        if (expectedSubject.equals(storedLocal)) {
            fallbackRefreshTokens.invalidate(jti);
            return true;
        }
        return false;
    }

    public void revokeRefreshToken(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        if (shouldUseRedis()) {
            try {
                stringRedisTemplate.delete(REFRESH_PREFIX + jti);
            } catch (DataAccessException ex) {
                log.warn("[Refresh Token Fallback] Lỗi xóa Redis cho jti='{}': {}", jti, ex.getMessage());
            }
        }
        fallbackRefreshTokens.invalidate(jti);
    }
}
