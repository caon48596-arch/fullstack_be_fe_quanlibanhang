package com.example.demo.service.contract;

/**
 * Hợp đồng sinh và xác thực mã OTP (DIP).
 */
public interface OtpOperations {

    String generateOtp(String identifier);

    void verifyOtp(String identifier, String otp);
}
