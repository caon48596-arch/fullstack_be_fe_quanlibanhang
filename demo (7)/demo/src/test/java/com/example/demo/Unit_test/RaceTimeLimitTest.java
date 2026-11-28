package com.example.demo.Unit_test;

import com.example.demo.security.RaceTimeLimit;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RaceTimeLimitTest {

    private RaceTimeLimit filter;

    @BeforeEach
    void setUp() {
        filter = new RaceTimeLimit();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "cacheType", "caffeine");
        ReflectionTestUtils.setField(filter, "authRequestsPerMinute", 2L);
        ReflectionTestUtils.setField(filter, "readRequestsPerSecond", 5L);
        ReflectionTestUtils.setField(filter, "defaultRequestsPerSecond", 3L);
        ReflectionTestUtils.setField(filter, "cacheMaxSize", 1000L);
        ReflectionTestUtils.setField(filter, "cacheExpireMinutes", 5L);
        filter.init();
    }

    @Test
    @DisplayName("Tách bucket theo IP thật từ X-Forwarded-For và chặn 429 khi vượt ngưỡng Auth (Caffeine fallback)")
    void testRateLimit_XForwardedFor_And429() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        // IP A gửi 2 request auth -> thành công
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/users/login");
            req.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }

        // Request thứ 3 từ IP A -> bị chặn 429
        MockHttpServletRequest reqBlocked = new MockHttpServletRequest("POST", "/api/users/login");
        reqBlocked.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        MockHttpServletResponse resBlocked = new MockHttpServletResponse();
        filter.doFilter(reqBlocked, resBlocked, chain);
        assertEquals(429, resBlocked.getStatus());
        assertNotNull(resBlocked.getHeader("Retry-After"));
        assertTrue(resBlocked.getContentAsString().contains("\"code\":\"429\""));

        // IP B qua cùng proxy 10.0.0.1 vẫn đi qua bình thường
        MockHttpServletRequest reqIpB = new MockHttpServletRequest("POST", "/api/users/login");
        reqIpB.addHeader("X-Forwarded-For", "198.51.100.25, 10.0.0.1");
        MockHttpServletResponse resIpB = new MockHttpServletResponse();
        filter.doFilter(reqIpB, resIpB, chain);
        assertEquals(200, resIpB.getStatus());
    }

    @Test
    @DisplayName("Rate Limit phân tán bằng Redis Lua Script và trả 429 khi vượt ngưỡng")
    @SuppressWarnings("unchecked")
    void testRateLimit_RedisPrimaryMode() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenReturn(List.of(1L, 60L))
                .thenReturn(List.of(2L, 58L))
                .thenReturn(List.of(3L, 55L));

        RaceTimeLimit redisFilter = new RaceTimeLimit();
        redisFilter.setStringRedisTemplate(redisTemplate);
        ReflectionTestUtils.setField(redisFilter, "enabled", true);
        ReflectionTestUtils.setField(redisFilter, "cacheType", "redis");
        ReflectionTestUtils.setField(redisFilter, "authRequestsPerMinute", 2L);
        redisFilter.init();

        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/users/login");
        req1.addHeader("X-Forwarded-For", "198.51.100.50");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        redisFilter.doFilter(req1, res1, chain);
        assertEquals(200, res1.getStatus());
        assertEquals("1", res1.getHeader("X-Rate-Limit-Remaining"));

        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/users/login");
        req2.addHeader("X-Forwarded-For", "198.51.100.50");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        redisFilter.doFilter(req2, res2, chain);
        assertEquals(200, res2.getStatus());
        assertEquals("0", res2.getHeader("X-Rate-Limit-Remaining"));

        MockHttpServletRequest req3 = new MockHttpServletRequest("POST", "/api/users/login");
        req3.addHeader("X-Forwarded-For", "198.51.100.50");
        MockHttpServletResponse res3 = new MockHttpServletResponse();
        redisFilter.doFilter(req3, res3, chain);
        assertEquals(429, res3.getStatus());
        assertEquals("55", res3.getHeader("Retry-After"));
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    @DisplayName("Fallback sang Caffeine/Bucket4j khi Redis tạm mất kết nối")
    @SuppressWarnings("unchecked")
    void testRateLimit_RedisDown_GracefulFallback() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new QueryTimeoutException("Redis down"));

        RaceTimeLimit redisFilter = new RaceTimeLimit();
        redisFilter.setStringRedisTemplate(redisTemplate);
        ReflectionTestUtils.setField(redisFilter, "enabled", true);
        ReflectionTestUtils.setField(redisFilter, "cacheType", "redis");
        ReflectionTestUtils.setField(redisFilter, "authRequestsPerMinute", 1L);
        redisFilter.init();

        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/users/login");
        req1.addHeader("X-Forwarded-For", "203.0.113.88");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        redisFilter.doFilter(req1, res1, chain);
        assertEquals(200, res1.getStatus());

        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/users/login");
        req2.addHeader("X-Forwarded-For", "203.0.113.88");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        redisFilter.doFilter(req2, res2, chain);
        assertEquals(429, res2.getStatus());
    }

    @Test
    @DisplayName("Bỏ qua CORS preflight OPTIONS và endpoint Health Check")
    void testRateLimit_SkipsOptionsAndHealth() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest healthReq = new MockHttpServletRequest("GET", "/api/v1/system/health");
            MockHttpServletResponse healthRes = new MockHttpServletResponse();
            filter.doFilter(healthReq, healthRes, chain);
            assertEquals(200, healthRes.getStatus());

            MockHttpServletRequest optReq = new MockHttpServletRequest("OPTIONS", "/api/users/login");
            MockHttpServletResponse optRes = new MockHttpServletResponse();
            filter.doFilter(optReq, optRes, chain);
            assertEquals(200, optRes.getStatus());
        }

        verify(chain, times(20)).doFilter(any(), any());
    }
}
