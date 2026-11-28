package com.example.demo.Unit_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.demo.service.OtpService;

class OtpServiceTest {

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
    }

    @Test
    @DisplayName("Tạo mã OTP 6 chữ số thành công (Caffeine fallback)")
    void testGenerateOtp_Success() {
        String identifier = "user@example.com";
        String otp = otpService.generateOtp(identifier);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"));
    }

    @Test
    @DisplayName("Tạo mã OTP thất bại khi identifier bị null hoặc rỗng")
    void testGenerateOtp_NullOrEmptyIdentifier_ThrowsException() {
        assertThrows(RuntimeException.class, () -> otpService.generateOtp(null));
        assertThrows(RuntimeException.class, () -> otpService.generateOtp("   "));
    }

    @Test
    @DisplayName("Xác thực mã OTP thành công")
    void testVerifyOtp_Success() {
        String identifier = "user@example.com";
        String otp = otpService.generateOtp(identifier);

        assertDoesNotThrow(() -> otpService.verifyOtp(identifier, otp));
    }

    @Test
    @DisplayName("Xác thực OTP thất bại khi thiếu thông tin")
    void testVerifyOtp_MissingInfo_ThrowsException() {
        assertThrows(RuntimeException.class, () -> otpService.verifyOtp(null, "123456"));
        assertThrows(RuntimeException.class, () -> otpService.verifyOtp("user@example.com", null));
    }

    @Test
    @DisplayName("Xác thực OTP thất bại khi mã OTP không tồn tại")
    void testVerifyOtp_NonExistentOtp_ThrowsException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            otpService.verifyOtp("unknown@example.com", "123456");
        });

        assertTrue(exception.getMessage().contains("không tồn tại hoặc đã hết hạn"));
    }

    @Test
    @DisplayName("Xác thực OTP thất bại khi nhập sai mã")
    void testVerifyOtp_WrongOtp_IncrementsAttempts() {
        String identifier = "user@example.com";
        otpService.generateOtp(identifier);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            otpService.verifyOtp(identifier, "999999");
        });

        assertTrue(exception.getMessage().contains("Mã OTP không chính xác"));
    }

    @Test
    @DisplayName("Xác thực OTP thất bại khi nhập sai quá 5 lần")
    void testVerifyOtp_ExceedsMaxAttempts_ThrowsException() {
        String identifier = "user@example.com";
        otpService.generateOtp(identifier);

        for (int i = 0; i < 4; i++) {
            assertThrows(RuntimeException.class, () -> otpService.verifyOtp(identifier, "000000"));
        }

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            otpService.verifyOtp(identifier, "000000");
        });

        assertTrue(exception.getMessage().contains("quá 5 lần"));
    }

    @Test
    @DisplayName("Lưu và xác thực OTP trên Redis (TTL 5 phút + atomic HINCRBY khi sai)")
    @SuppressWarnings("unchecked")
    void testOtp_RedisPrimaryMode() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        OtpService redisOtpService = new OtpService();
        redisOtpService.setStringRedisTemplate(redisTemplate);
        redisOtpService.setCacheType("redis");

        String otp = redisOtpService.generateOtp("User@Example.com");
        String expectedKey = "ipworld:otp:user@example.com";

        verify(hashOps).putAll(eq(expectedKey), eq(Map.of("otp", otp, "attempts", "0")));
        verify(redisTemplate).expire(eq(expectedKey), eq(Duration.ofSeconds(300)));

        // Nhập sai 1 lần -> tăng attempts atomic bằng HINCRBY
        when(hashOps.get(expectedKey, "otp")).thenReturn(otp);
        when(hashOps.increment(expectedKey, "attempts", 1L)).thenReturn(1L);
        RuntimeException wrongEx = assertThrows(RuntimeException.class,
                () -> redisOtpService.verifyOtp("user@example.com", "000000"));
        assertTrue(wrongEx.getMessage().contains("còn 4 lần thử"));

        // Nhập đúng -> xóa key khỏi Redis
        assertDoesNotThrow(() -> redisOtpService.verifyOtp("user@example.com", otp));
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("Tự động fallback sang Caffeine nếu Redis mất kết nối")
    @SuppressWarnings("unchecked")
    void testOtp_RedisDown_GracefulFallbackToCaffeine() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        doThrow(new QueryTimeoutException("Redis unreachable")).when(hashOps).putAll(anyString(), anyMap());
        when(hashOps.get(anyString(), any())).thenThrow(new QueryTimeoutException("Redis unreachable"));

        OtpService redisOtpService = new OtpService();
        redisOtpService.setStringRedisTemplate(redisTemplate);
        redisOtpService.setCacheType("redis");

        String otp = redisOtpService.generateOtp("fallback@example.com");
        assertDoesNotThrow(() -> redisOtpService.verifyOtp("fallback@example.com", otp));
    }
}
