package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtpServiceTest {

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService();
    }

    @Test
    @DisplayName("Tạo mã OTP 6 chữ số thành công")
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

        // Thử sai 4 lần
        for (int i = 0; i < 4; i++) {
            assertThrows(RuntimeException.class, () -> otpService.verifyOtp(identifier, "000000"));
        }

        // Lần thứ 5 sẽ báo bị khóa/nhập sai quá 5 lần
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            otpService.verifyOtp(identifier, "000000");
        });

        assertTrue(exception.getMessage().contains("quá 5 lần"));
    }
}
