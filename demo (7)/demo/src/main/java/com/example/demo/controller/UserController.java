package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Repository.UserRepository;
import com.example.demo.dto.ResponseDTO;
import com.example.demo.dto.UserDTO.OtpLoginDTO;
import com.example.demo.dto.UserDTO.OtpRequestDTO;
import com.example.demo.dto.UserDTO.UserLoginDTO;
import com.example.demo.dto.UserDTO.UserLogoutDTO;
import com.example.demo.dto.UserDTO.UserRegisterDTO;
import com.example.demo.dto.UserDTO.UserUpdateDTO;
import com.example.demo.entity.User;
import com.example.demo.service.EmailService;
import com.example.demo.service.OtpService;
import com.example.demo.service.UserService;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/users", "/api"})
public class UserController {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseDTO<String>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage(), "400"));
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/request-otp")
    public ResponseEntity<ResponseDTO<String>> requestOtp(@RequestBody OtpRequestDTO otpRequest) {
        String identifier = otpRequest.getEmail();
        String type = otpRequest.getType();

        if (identifier == null || identifier.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseDTO.error("Vui lòng cung cấp email hoặc số điện thoại", "400"));
        }

        boolean exists = userRepository.findByEmail(identifier).isPresent() ||
                         userRepository.findByPhone(identifier).isPresent();

        if ("REGISTER".equalsIgnoreCase(type)) {
            if (exists) {
                return ResponseEntity.badRequest().body(ResponseDTO.error("Email hoặc SĐT đã tồn tại trong hệ thống", "400"));
            }
        } else if ("LOGIN".equalsIgnoreCase(type)) {
            if (!exists) {
                return ResponseEntity.badRequest().body(ResponseDTO.error("Email hoặc SĐT không tồn tại trong hệ thống", "400"));
            }
        }

        String otp = otpService.generateOtp(identifier);
        emailService.sendOtpEmail(identifier, otp);

        return ResponseEntity.ok(ResponseDTO.success("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra hộp thư."));
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<String>> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        try {
            if (userRegisterDTO.getOtp() != null && !userRegisterDTO.getOtp().trim().isEmpty()) {
                otpService.verifyOtp(userRegisterDTO.getEmail(), userRegisterDTO.getOtp());
            } else {
                return ResponseEntity.badRequest().body(ResponseDTO.error("Vui lòng nhập mã OTP để xác thực đăng ký", "400"));
            }

            userService.registerUser(userRegisterDTO);
            return ResponseEntity.ok(ResponseDTO.success("Đăng ký thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage(), "400"));
        }
    }

    @PostMapping("/login-otp")
    public ResponseEntity<ResponseDTO<?>> loginWithOtp(@RequestBody OtpLoginDTO loginDTO) {
        String identifier = loginDTO.getEmail();
        String password = loginDTO.getPassword();

        try {
            otpService.verifyOtp(identifier, loginDTO.getOtp());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ResponseDTO.error(e.getMessage(), "401"));
        }

        try {
            UserLoginDTO result = userService.login(identifier, password);
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ResponseDTO.error(e.getMessage(), "401"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<?>> login(@RequestBody UserLoginDTO userLoginDTO) {
        try {
            UserLoginDTO result = userService.login(userLoginDTO.getEmail(), userLoginDTO.getPassword());
            return ResponseEntity.ok(ResponseDTO.success(result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(ResponseDTO.error("Sai tài khoản hoặc mật khẩu", "401"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO<String>> logout(
            @RequestBody(required = false) UserLogoutDTO userLogoutDTO,
            Principal principal) {
        try {
            if (principal != null && principal.getName() != null) {
                userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                    user.setOnline(false);
                    userRepository.save(user);
                });
            } else if (userLogoutDTO != null && userLogoutDTO.getId() != null) {
                userService.logout(userLogoutDTO.getId());
            }
            return ResponseEntity.ok(ResponseDTO.success("Đăng xuất thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(ResponseDTO.error("Đăng xuất thất bại", "400"));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ResponseDTO<?>> getProfile(
            @RequestParam(required = false) String email,
            Principal principal) {
        try {
            // Ưu tiên email từ JWT Principal để tránh truy cập trái phép thông tin tài khoản khác
            String targetEmail = (principal != null && principal.getName() != null) ? principal.getName() : email;
            if (targetEmail == null || targetEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ResponseDTO.error("Thiếu thông tin người dùng", "400"));
            }

            User user = userRepository.findByEmail(targetEmail)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Map<String, String> profile = new HashMap<>();
            profile.put("email", user.getEmail());
            profile.put("phone", user.getPhone());
            profile.put("dob", user.getDob());
            profile.put("username", user.getUsername());
            profile.put("role", user.getRole());

            return ResponseEntity.ok(ResponseDTO.success(profile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ResponseDTO.error(e.getMessage(), "400"));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseDTO<?>> update(
            @RequestBody UserUpdateDTO userUpdateDTO,
            Principal principal) {
        try {
            // Ưu tiên email từ JWT Principal để tránh chỉnh sửa tài khoản người khác
            String targetEmail = (principal != null && principal.getName() != null)
                    ? principal.getName()
                    : userUpdateDTO.getEmail();

            if (targetEmail == null || targetEmail.trim().isEmpty()) {
                return ResponseEntity.status(400).body(ResponseDTO.error("Thiếu email người dùng", "400"));
            }

            userService.updateUser(targetEmail, userUpdateDTO);
            return ResponseEntity.ok(ResponseDTO.success("Cập nhật thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(ResponseDTO.error(e.getMessage(), "400"));
        }
    }
}
