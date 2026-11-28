package com.example.demo.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RaceTimeLimit extends OncePerRequestFilter {

    private static final String REDIS_KEY_PREFIX = "ipworld:ratelimit:";

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> RATE_LIMIT_LUA_SCRIPT = createRateLimitLuaScript();

    @Value("${app.ratelimit.enabled:true}")
    private boolean enabled = true;

    @Value("${app.cache.type:redis}")
    private String cacheType = "redis";

    @Value("${app.ratelimit.auth-requests-per-minute:15}")
    private long authRequestsPerMinute = 15L;

    @Value("${app.ratelimit.read-requests-per-second:60}")
    private long readRequestsPerSecond = 60L;

    @Value("${app.ratelimit.default-requests-per-second:20}")
    private long defaultRequestsPerSecond = 20L;

    @Value("${app.ratelimit.cache-max-size:50000}")
    private long cacheMaxSize = 50_000L;

    @Value("${app.ratelimit.cache-expire-minutes:10}")
    private long cacheExpireMinutes = 10L;

    private StringRedisTemplate stringRedisTemplate;
    private Cache<String, Bucket> fallbackBucketCache;

    @Autowired(required = false)
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @SuppressWarnings("rawtypes")
    private static DefaultRedisScript<List> createRateLimitLuaScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(
                "local current = redis.call('INCR', KEYS[1]) " +
                "if current == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end " +
                "local ttl = redis.call('TTL', KEYS[1]) " +
                "if ttl < 0 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) ttl = tonumber(ARGV[1]) end " +
                "return {current, ttl}"
        );
        script.setResultType(List.class);
        return script;
    }

    @PostConstruct
    public void init() {
        this.fallbackBucketCache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterAccess(cacheExpireMinutes, TimeUnit.MINUTES)
                .build();
    }

    private enum RateLimitTier {
        AUTH,
        PUBLIC_READ,
        DEFAULT
    }

    private record RateLimitDecision(boolean allowed, long remainingTokens, long waitForRefillSeconds) {
        static RateLimitDecision allow(long remaining) {
            return new RateLimitDecision(true, Math.max(0L, remaining), 0L);
        }

        static RateLimitDecision reject(long retryAfterSeconds) {
            return new RateLimitDecision(false, 0L, Math.max(1L, retryAfterSeconds));
        }
    }

    private boolean shouldUseRedis() {
        return stringRedisTemplate != null && "redis".equalsIgnoreCase(cacheType);
    }

    private RateLimitTier resolveTier(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/api/users/login")
                || path.startsWith("/api/login")
                || path.startsWith("/api/users/register")
                || path.startsWith("/api/users/request-otp")
                || path.startsWith("/api/users/login-otp")) {
            return RateLimitTier.AUTH;
        }

        if ("GET".equalsIgnoreCase(method)
                && (path.startsWith("/api/products") || path.startsWith("/api/categories"))) {
            return RateLimitTier.PUBLIC_READ;
        }

        return RateLimitTier.DEFAULT;
    }

    private long getLimitForTier(RateLimitTier tier) {
        return switch (tier) {
            case AUTH -> authRequestsPerMinute;
            case PUBLIC_READ -> readRequestsPerSecond;
            case DEFAULT -> defaultRequestsPerSecond;
        };
    }

    private long getWindowSecondsForTier(RateLimitTier tier) {
        return tier == RateLimitTier.AUTH ? 60L : 1L;
    }

    private Bucket createLocalBucket(RateLimitTier tier) {
        Bandwidth limit = switch (tier) {
            case AUTH -> Bandwidth.classic(
                    authRequestsPerMinute,
                    Refill.greedy(authRequestsPerMinute, Duration.ofMinutes(1)));
            case PUBLIC_READ -> Bandwidth.classic(
                    readRequestsPerSecond,
                    Refill.greedy(readRequestsPerSecond, Duration.ofSeconds(1)));
            case DEFAULT -> Bandwidth.classic(
                    defaultRequestsPerSecond,
                    Refill.greedy(defaultRequestsPerSecond, Duration.ofSeconds(1)));
        };
        return Bucket.builder().addLimit(limit).build();
    }

    private RateLimitDecision tryConsumeRedis(RateLimitTier tier, String clientIp) {
        String redisKey = REDIS_KEY_PREFIX + tier.name() + ":" + clientIp;
        long limit = getLimitForTier(tier);
        long windowSeconds = getWindowSecondsForTier(tier);

        try {
            List<?> result = stringRedisTemplate.execute(
                    RATE_LIMIT_LUA_SCRIPT,
                    List.of(redisKey),
                    String.valueOf(windowSeconds));

            if (result != null && result.size() >= 2
                    && result.get(0) instanceof Number currentNum
                    && result.get(1) instanceof Number ttlNum) {
                long current = currentNum.longValue();
                long ttl = ttlNum.longValue();
                if (current <= limit) {
                    return RateLimitDecision.allow(limit - current);
                }
                return RateLimitDecision.reject(Math.max(1L, ttl));
            }
        } catch (DataAccessException ex) {
            log.warn("[RateLimit Redis Fallback] Lỗi kết nối Redis cho key '{}' (fallback Caffeine): {}",
                    redisKey, ex.getMessage());
        }
        return null;
    }

    private RateLimitDecision tryConsumeLocal(RateLimitTier tier, String clientIp) {
        if (fallbackBucketCache == null) {
            init();
        }
        String cacheKey = tier.name() + ":" + clientIp;
        Bucket bucket = fallbackBucketCache.get(cacheKey, k -> createLocalBucket(tier));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitDecision.allow(probe.getRemainingTokens());
        }
        long waitSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        return RateLimitDecision.reject(waitSeconds);
    }

    String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Lấy IP client đầu tiên trong chuỗi proxy (client, proxy1, proxy2)
            String firstIp = forwardedFor.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String method = request.getMethod();
        String path = request.getRequestURI();
        // Bỏ qua CORS preflight, health check probe và Swagger docs
        return "OPTIONS".equalsIgnoreCase(method)
                || "/api/v1/system/health".equals(path)
                || path.startsWith("/actuator/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = extractClientIp(request);
        RateLimitTier tier = resolveTier(request);

        RateLimitDecision decision = shouldUseRedis() ? tryConsumeRedis(tier, clientIp) : null;
        if (decision == null) {
            decision = tryConsumeLocal(tier, clientIp);
        }

        if (decision.allowed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(decision.remainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long waitForRefillSeconds = decision.waitForRefillSeconds();
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(waitForRefillSeconds));
        response.setHeader("X-Rate-Limit-Remaining", "0");
        response.getWriter().write(
                "{\"code\":\"429\",\"message\":\"Quá nhiều yêu cầu từ IP của bạn, vui lòng thử lại sau "
                        + waitForRefillSeconds + " giây.\"}");
    }
}
