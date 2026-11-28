package com.example.demo.service.contract;

import com.example.demo.dto.UserDTO.OtpLoginDTO;
import com.example.demo.dto.UserDTO.OtpRequestDTO;
import com.example.demo.dto.UserDTO.UserLoginDTO;
import com.example.demo.dto.UserDTO.UserRegisterDTO;

/**
 * Hợp đồng điều phối Xác thực, OTP và Quản lý phiên JWT/Refresh Token (SRP, ISP, DIP).
 */
public interface AuthOperations {

    record AuthSessionResult(UserLoginDTO loginData, String refreshToken, long refreshExpirationMs) {}

    void requestOtp(OtpRequestDTO otpRequest);

    void registerWithOtp(UserRegisterDTO userRegisterDTO);

    AuthSessionResult loginWithPassword(UserLoginDTO userLoginDTO);

    AuthSessionResult loginWithOtp(OtpLoginDTO otpLoginDTO);

    AuthSessionResult refreshSession(String refreshToken);

    void logoutSession(String authorizationHeader, String refreshToken, String principalEmail, Long fallbackUserId);
}
