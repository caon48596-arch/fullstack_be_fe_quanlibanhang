package com.example.demo.controller;

import com.example.demo.dto.ResponseDTO;
import com.example.demo.dto.UserDTO.OtpLoginDTO;
import com.example.demo.dto.UserDTO.OtpRequestDTO;
import com.example.demo.dto.UserDTO.UserLoginDTO;
import com.example.demo.dto.UserDTO.UserLogoutDTO;
import com.example.demo.dto.UserDTO.UserRegisterDTO;
import com.example.demo.dto.UserDTO.UserUpdateDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.security.RefreshTokenCookieHelper;
import com.example.demo.service.contract.AuthOperations;
import com.example.demo.service.contract.AuthOperations.AuthSessionResult;
import com.example.demo.service.contract.UserOperations;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

/**
 * Thin HTTP Controller cho Người dùng & Xác thực (SRP, ISP, DIP):
 * - Không truy cập trực tiếp UserRepository, JwtUtil, OtpService hay EmailService.
 * - Toàn bộ nghiệp vụ Auth/OTP/Rotation được ủy quyền cho {@link AuthOperations},
 *   thông tin hồ sơ ủy quyền cho {@link UserOperations}, và Cookie HTTP giao cho {@link RefreshTokenCookieHelper}.
 */
@RestController
@RequestMapping({"/api/users", "/api"})
@RequiredArgsConstructor
public class UserController {

    private final AuthOperations authService;
    private final UserOperations userService;
    private final RefreshTokenCookieHelper cookieHelper;

    @PostMapping("/request-otp")
    public ResponseEntity<ResponseDTO<String>> requestOtp(@RequestBody OtpRequestDTO otpRequest) {
        authService.requestOtp(otpRequest);
        return ResponseEntity.ok(ResponseDTO.success("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư."));
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<String>> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        authService.registerWithOtp(userRegisterDTO);
        return ResponseEntity.ok(ResponseDTO.success("Đăng ký thành công"));
    }

    @PostMapping("/login-otp")
    public ResponseEntity<ResponseDTO<UserLoginDTO>> loginWithOtp(
            @RequestBody OtpLoginDTO loginDTO,
            HttpServletResponse response) {
        AuthSessionResult session = authService.loginWithOtp(loginDTO);
        cookieHelper.writeRefreshCookie(response, session.refreshToken(), session.refreshExpirationMs());
        return ResponseEntity.ok(ResponseDTO.success(session.loginData()));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<UserLoginDTO>> login(
            @RequestBody UserLoginDTO userLoginDTO,
            HttpServletResponse response) {
        AuthSessionResult session = authService.loginWithPassword(userLoginDTO);
        cookieHelper.writeRefreshCookie(response, session.refreshToken(), session.refreshExpirationMs());
        return ResponseEntity.ok(ResponseDTO.success(session.loginData()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseDTO<UserLoginDTO>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody(required = false) Map<String, String> body) {
        String refreshToken = cookieHelper.extractRefreshCookie(request);
        if (!StringUtils.hasText(refreshToken) && body != null) {
            refreshToken = body.get("refreshToken");
        }

        try {
            AuthSessionResult refreshedSession = authService.refreshSession(refreshToken);
            cookieHelper.writeRefreshCookie(
                    response,
                    refreshedSession.refreshToken(),
                    refreshedSession.refreshExpirationMs());
            return ResponseEntity.ok(ResponseDTO.success(refreshedSession.loginData()));
        } catch (UnauthorizedException ex) {
            cookieHelper.clearRefreshCookie(response);
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO<String>> logout(
            @RequestBody(required = false) UserLogoutDTO userLogoutDTO,
            Principal principal,
            HttpServletRequest request,
            HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");
        String refreshToken = cookieHelper.extractRefreshCookie(request);
        String principalEmail = principal != null ? principal.getName() : null;
        Long fallbackId = userLogoutDTO != null ? userLogoutDTO.getId() : null;

        authService.logoutSession(authHeader, refreshToken, principalEmail, fallbackId);
        cookieHelper.clearRefreshCookie(response);

        return ResponseEntity.ok(ResponseDTO.success("Đăng xuất thành công"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ResponseDTO<Map<String, String>>> getProfile(
            @RequestParam(required = false) String email,
            Principal principal) {
        String targetEmail = (principal != null && principal.getName() != null) ? principal.getName() : email;
        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            throw new BusinessException("Thiếu thông tin người dùng");
        }
        return ResponseEntity.ok(ResponseDTO.success(userService.getUserProfile(targetEmail)));
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<String>> update(
            @RequestBody UserUpdateDTO userUpdateDTO,
            Principal principal) {
        String targetEmail = (principal != null && principal.getName() != null)
                ? principal.getName()
                : userUpdateDTO.getEmail();

        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            throw new BusinessException("Thiếu email người dùng");
        }

        userService.updateUser(targetEmail, userUpdateDTO);
        return ResponseEntity.ok(ResponseDTO.success("Cập nhật thành công!"));
    }
}
