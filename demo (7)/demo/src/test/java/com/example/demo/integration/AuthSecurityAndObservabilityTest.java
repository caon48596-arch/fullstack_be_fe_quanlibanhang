package com.example.demo.integration;

import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("JWT Refresh Rotation, Logout Blacklist, Correlation ID & Observability Tests")
public class AuthSecurityAndObservabilityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Login cấp HttpOnly Refresh Cookie -> Refresh Rotation chống replay -> Logout thu hồi Access Token ngay lập tức")
    void testRefreshTokenRotationAndLogoutBlacklist() throws Exception {
        String email = "auth_sec_" + System.nanoTime() + "@test.com";
        User user = new User();
        user.setEmail(email);
        user.setUsername("Security Tester");
        user.setPassword(passwordEncoder.encode("Pass@12345"));
        user.setRole("ROLE_CLIENT");
        userRepository.save(user);

        // 1. Login -> nhận Access Token + HttpOnly Cookie `refreshToken` + Header `X-Request-Id`
        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "Pass@12345"))))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn();

        String accessToken1 = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();
        Cookie refreshCookie1 = loginResult.getResponse().getCookie("refreshToken");
        assertNotNull(refreshCookie1, "Phải cấp HttpOnly refreshToken cookie khi đăng nhập");
        assertTrue(refreshCookie1.isHttpOnly(), "refreshToken cookie phải bật HttpOnly");

        // 2. Gọi /api/users/refresh -> đổi được Access Token mới + Refresh Cookie mới (Rotation)
        MvcResult refreshResult = mockMvc.perform(post("/api/users/refresh")
                        .cookie(refreshCookie1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn();

        String accessToken2 = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .path("data").path("token").asText();
        Cookie refreshCookie2 = refreshResult.getResponse().getCookie("refreshToken");
        assertNotNull(refreshCookie2);
        assertNotEquals(refreshCookie1.getValue(), refreshCookie2.getValue());

        // 3. Replay lại refreshCookie1 cũ -> bị từ chối 401 Unauthorized (chống đánh cắp token)
        mockMvc.perform(post("/api/users/refresh")
                        .cookie(refreshCookie1))
                .andExpect(status().isUnauthorized());

        // 4. Access Token mới dùng được trước khi Logout
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().isOk());

        // 5. Logout -> thu hồi Access Token (Redis/Caffeine Blacklist) & Refresh Token
        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", "Bearer " + accessToken2)
                        .cookie(refreshCookie2))
                .andExpect(status().isOk());

        // 6. Access Token cũ ngay lập tức mất hiệu lực sau Logout (401/403)
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + accessToken2))
                .andExpect(status().is4xxClientError());

        // 7. Refresh Cookie đã bị thu hồi khi Logout cũng không thể refresh được nữa
        mockMvc.perform(post("/api/users/refresh")
                        .cookie(refreshCookie2))
                .andExpect(status().isUnauthorized());

        assertNotNull(accessToken1);
    }

    @Test
    @DisplayName("Actuator Health, OpenAPI Spec & X-Request-Id Tracing hoạt động ổn định")
    void testObservabilityEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Request-Id", "trace-demo-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "trace-demo-001"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", notNullValue()));
    }
}
