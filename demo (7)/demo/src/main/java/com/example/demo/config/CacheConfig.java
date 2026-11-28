package com.example.demo.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching(proxyTargetClass = true)
@Slf4j
public class CacheConfig implements CachingConfigurer {

    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_PRODUCT_DETAIL = "productDetail";
    public static final String CACHE_PRODUCTS_BY_CATEGORY = "productsByCategory";
    public static final String CACHE_DASHBOARD_STATS = "dashboardStats";

    @Value("${app.cache.type:redis}")
    private String cacheType;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        if ("caffeine".equalsIgnoreCase(cacheType)) {
            log.info("[Cache] Sử dụng CaffeineCacheManager cục bộ (categories, productDetail, productsByCategory, dashboardStats)");
            CaffeineCacheManager manager = new CaffeineCacheManager();
            manager.setCacheNames(List.of(
                    CACHE_CATEGORIES,
                    CACHE_PRODUCT_DETAIL,
                    CACHE_PRODUCTS_BY_CATEGORY,
                    CACHE_DASHBOARD_STATS));
            manager.setCaffeine(Caffeine.newBuilder()
                    .maximumSize(5_000)
                    .expireAfterWrite(10, TimeUnit.MINUTES));
            return manager;
        }

        log.info("[Cache] Khởi tạo RedisCacheManager (1 node) với TTL phân tầng theo từng vùng dữ liệu");

        ObjectMapper redisMapper = new ObjectMapper();
        redisMapper.registerModule(new JavaTimeModule());
        redisMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        redisMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("ipworld::")
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CACHE_CATEGORIES, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put(CACHE_PRODUCT_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put(CACHE_PRODUCTS_BY_CATEGORY, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put(CACHE_DASHBOARD_STATS, defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    /**
     * Graceful Degradation: Nếu Redis tạm thời mất kết nối, ghi log cảnh báo và tự fallback truy vấn thẳng DB
     * thay vì làm lỗi HTTP 500 cho người dùng.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[Cache Fallback] Không thể đọc cache '{}' key='{}' (fallback DB): {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("[Cache Fallback] Không thể ghi cache '{}' key='{}': {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("[Cache Fallback] Không thể xóa cache '{}' key='{}': {}",
                        cache != null ? cache.getName() : "unknown", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("[Cache Fallback] Không thể clear cache '{}': {}",
                        cache != null ? cache.getName() : "unknown", exception.getMessage());
            }
        };
    }
}
