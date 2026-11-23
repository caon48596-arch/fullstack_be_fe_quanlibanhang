package com.example.demo.Api_test;

import com.example.demo.service.OtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API Test - User & Authentication Service")
public class UserApiTest extends BaseApiTest {

    @Autowired
    private OtpService otpService;

    @Test
    @DisplayName("POST /api/users/request-otp - Yêu cầu OTP đăng ký với email mới thành công")
    void testRequestOtp_ForRegister_Success() throws Exception {
        Map<String, String> body = Map.of(
                "email", "newuser_otp@test.com",
                "type", "REGISTER"
        );

        mockMvc.perform(post("/api/users/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("POST /api/users/request-otp - Yêu cầu OTP đăng ký lỗi khi email đã tồn tại (400)")
    void testRequestOtp_ForRegister_EmailExists() throws Exception {
        Map<String, String> body = Map.of(
                "email", clientUser.getEmail(),
                "type", "REGISTER"
        );

        mockMvc.perform(post("/api/users/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("400")));
    }

    @Test
    @DisplayName("POST /api/users/request-otp - Yêu cầu OTP đăng nhập cho tài khoản tồn tại thành công")
    void testRequestOtp_ForLogin_Success() throws Exception {
        Map<String, String> body = Map.of(
                "email", clientUser.getEmail(),
                "type", "LOGIN"
        );

        mockMvc.perform(post("/api/users/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("POST /api/users/register - Đăng ký tài khoản thành công kèm OTP hợp lệ")
    void testRegisterUser_Success() throws Exception {
        String registerEmail = "register_test@test.com";
        String otp = otpService.generateOtp(registerEmail);

        Map<String, String> body = new HashMap<>();
        body.put("email", registerEmail);
        body.put("password", "Pass123456");
        body.put("username", "User Dang Ky");
        body.put("phone", "0900112233");
        body.put("dob", "1995-05-05");
        body.put("otp", otp);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));

        assertTrue(userRepository.findByEmail(registerEmail).isPresent());
    }

    @Test
    @DisplayName("POST /api/users/register - Đăng ký thất bại khi thiếu mã OTP (400)")
    void testRegisterUser_MissingOtp() throws Exception {
        Map<String, String> body = Map.of(
                "email", "no_otp@test.com",
                "password", "Pass123456"
        );

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("400")));
    }

    @Test
    @DisplayName("POST /api/users/login - Đăng nhập tài khoản thường thành công trả về JWT Token")
    void testLogin_Success() throws Exception {
        Map<String, String> body = Map.of(
                "email", clientUser.getEmail(),
                "password", "client123"
        );

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.email", is(clientUser.getEmail())));
    }

    @Test
    @DisplayName("POST /api/users/login - Đăng nhập sai mật khẩu trả về 401 Unauthorized")
    void testLogin_WrongPassword() throws Exception {
        Map<String, String> body = Map.of(
                "email", clientUser.getEmail(),
                "password", "saimatkhau"
        );

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/users/login-otp - Đăng nhập bằng mã xác thực OTP thành công")
    void testLoginWithOtp_Success() throws Exception {
        String otp = otpService.generateOtp(clientUser.getEmail());

        Map<String, String> body = Map.of(
                "email", clientUser.getEmail(),
                "password", "client123",
                "otp", otp
        );

        mockMvc.perform(post("/api/users/login-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/users/profile - Lấy thông tin cá nhân của người dùng đã xác thực")
    void testGetProfile_Success() throws Exception {
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is(clientUser.getEmail())))
                .andExpect(jsonPath("$.data.role", is("ROLE_CLIENT")));
    }

    @Test
    @DisplayName("GET /api/users/profile - Bị chặn khi không có Authorization Token (401/403)")
    void testGetProfile_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("PUT /api/users/update - Cập nhật thông tin cá nhân thành công")
    void testUpdateProfile_Success() throws Exception {
        Map<String, String> body = Map.of(
                "email", clientUser.getEmail(),
                "username", "Client Moi Cap Nhat",
                "phone", "0999888777",
                "dob", "2001-02-02"
        );

        mockMvc.perform(put("/api/users/update")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }

    @Test
    @DisplayName("POST /api/users/logout - Đăng xuất tài khoản thành công")
    void testLogout_Success() throws Exception {
        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")));
    }
}
