package com.example.demo.service;

import com.example.demo.dto.UserDTO.OtpLoginDTO;
import com.example.demo.dto.UserDTO.OtpRequestDTO;
import com.example.demo.dto.UserDTO.UserLoginDTO;
import com.example.demo.dto.UserDTO.UserRegisterDTO;
import com.example.demo.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.TokenProvider;
import com.example.demo.security.TokenRevocationOperations;
import com.example.demo.service.contract.AuthOperations;
import com.example.demo.service.contract.OtpNotificationSender;
import com.example.demo.service.contract.OtpOperations;
import com.example.demo.service.contract.UserOperations;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Điều phối tập trung nghiệp vụ Xác thực, OTP, Refresh Token Rotation và Logout Revocation,
 * giúp giải phóng UserController khỏi logic bảo mật/hạ tầng phức tạp (SRP, ISP, DIP).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService implements AuthOperations {

    private final UserOperations userOperations;
    private final OtpOperations otpOperations;
    private final OtpNotificationSender otpNotificationSender;
    private final TokenProvider tokenProvider;
    private final TokenRevocationOperations tokenRevocationOperations;

    @Override
    @Transactional(readOnly = true)
    public void requestOtp(OtpRequestDTO otpRequest) {
        String identifier = otpRequest != null ? otpRequest.getEmail() : null;
        String type = otpRequest != null ? otpRequest.getType() : null;

        if (identifier == null || identifier.trim().isEmpty()) {
            throw new BusinessException("Vui lòng cung cấp email hoặc số điện thoại");
        }

        boolean exists = userOperations.existsByIdentifier(identifier);
        if ("REGISTER".equalsIgnoreCase(type) && exists) {
            throw new BusinessException("Email hoặc SĐT đã tồn tại trong hệ thống");
        } else if ("LOGIN".equalsIgnoreCase(type) && !exists) {
            throw new BusinessException("Email hoặc SĐT không tồn tại trong hệ thống");
        }

        String otp = otpOperations.generateOtp(identifier);
        otpNotificationSender.sendOtpEmail(identifier, otp);
    }

    @Override
    public void registerWithOtp(UserRegisterDTO userRegisterDTO) {
        if (userRegisterDTO == null || userRegisterDTO.getOtp() == null || userRegisterDTO.getOtp().trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập mã OTP để xác thực đăng ký");
        }
        otpOperations.verifyOtp(userRegisterDTO.getEmail(), userRegisterDTO.getOtp());
        userOperations.registerUser(userRegisterDTO);
    }

    @Override
    public AuthSessionResult loginWithPassword(UserLoginDTO userLoginDTO) {
        try {
            UserLoginDTO result = userOperations.login(userLoginDTO.getEmail(), userLoginDTO.getPassword());
            return buildSessionForLoginResult(result);
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Sai tài khoản hoặc mật khẩu");
        }
    }

    @Override
    public AuthSessionResult loginWithOtp(OtpLoginDTO otpLoginDTO) {
        try {
            otpOperations.verifyOtp(otpLoginDTO.getEmail(), otpLoginDTO.getOtp());
        } catch (RuntimeException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }

        try {
            UserLoginDTO result = userOperations.login(otpLoginDTO.getEmail(), otpLoginDTO.getPassword());
            return buildSessionForLoginResult(result);
        } catch (RuntimeException ex) {
            throw new UnauthorizedException(ex.getMessage());
        }
    }

    @Override
    public AuthSessionResult refreshSession(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException("Thiếu Refresh Token. Vui lòng đăng nhập lại.");
        }

        Claims claims;
        try {
            claims = tokenProvider.parseClaims(refreshToken);
        } catch (Exception ex) {
            throw new UnauthorizedException("Refresh Token không hợp lệ hoặc đã hết hạn.");
        }

        if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(tokenProvider.extractTokenType(claims))) {
            throw new UnauthorizedException("Loại token không hợp lệ cho thao tác làm mới phiên.");
        }

        String subject = claims.getSubject();
        String jti = tokenProvider.extractJti(claims);

        if (!tokenRevocationOperations.consumeRefreshToken(jti, subject)) {
            throw new UnauthorizedException("Refresh Token đã bị thu hồi hoặc đã được sử dụng.");
        }

        User user = userOperations.findByIdentifier(subject)
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không còn tồn tại."));

        String role = StringUtils.hasText(user.getRole()) ? user.getRole() : "ROLE_CLIENT";
        String newAccessToken = tokenProvider.generateAccessToken(subject, role);
        String newRefreshToken = issueAndRegisterRefreshToken(subject, role);

        UserLoginDTO refreshed = new UserLoginDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail() != null ? user.getEmail() : user.getPhone(),
                null,
                newAccessToken);

        return new AuthSessionResult(refreshed, newRefreshToken, tokenProvider.getRefreshExpirationMs());
    }

    @Override
    public void logoutSession(String authorizationHeader, String refreshToken, String principalEmail, Long fallbackUserId) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            try {
                Claims accessClaims = tokenProvider.parseClaims(authorizationHeader.substring(7));
                tokenRevocationOperations.revokeAccessToken(
                        tokenProvider.extractJti(accessClaims),
                        accessClaims.getExpiration());
            } catch (Exception ignored) {
                // Token đã hết hạn hoặc không hợp lệ -> bỏ qua
            }
        }

        if (StringUtils.hasText(refreshToken)) {
            try {
                Claims refreshClaims = tokenProvider.parseClaims(refreshToken);
                tokenRevocationOperations.revokeRefreshToken(tokenProvider.extractJti(refreshClaims));
            } catch (Exception ignored) {
                // Bỏ qua nếu refresh token đã hết hạn
            }
        }

        if (StringUtils.hasText(principalEmail)) {
            userOperations.markOfflineByEmail(principalEmail);
        } else if (fallbackUserId != null) {
            userOperations.logout(fallbackUserId);
        }
    }

    private AuthSessionResult buildSessionForLoginResult(UserLoginDTO result) {
        String role = tokenProvider.extractRole(result.getToken());
        String subject = tokenProvider.extractUsername(result.getToken());
        String effectiveRole = StringUtils.hasText(role) ? role : "ROLE_CLIENT";
        String refreshToken = issueAndRegisterRefreshToken(subject, effectiveRole);
        return new AuthSessionResult(result, refreshToken, tokenProvider.getRefreshExpirationMs());
    }

    private String issueAndRegisterRefreshToken(String subject, String role) {
        String refreshToken = tokenProvider.generateRefreshToken(subject, role);
        Claims refreshClaims = tokenProvider.parseClaims(refreshToken);
        tokenRevocationOperations.registerRefreshToken(
                tokenProvider.extractJti(refreshClaims),
                subject,
                tokenProvider.getRefreshExpirationMs());
        return refreshToken;
    }
}
