package com.example.demo.integration;

import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.transaction.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Authentication Integration Tests")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.findByEmail("khachhang_auth@gmail.com")
                .ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Should login successfully and return JWT token when credentials are valid")
    void testLogin_Success() throws Exception {
        User user = new User();
        user.setEmail("khachhang_auth@gmail.com");
        user.setPassword(passwordEncoder.encode("matkhau123"));
        user.setRole("ROLE_CLIENT");
        userRepository.save(user);

        String requestBody = "{\"email\":\"khachhang_auth@gmail.com\",\"password\":\"matkhau123\"}";

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when password is incorrect")
    void testLogin_InvalidPassword_ShouldReturnUnauthorized() throws Exception {
        User user = new User();
        user.setEmail("khachhang_auth@gmail.com");
        user.setPassword(passwordEncoder.encode("matkhau123"));
        user.setRole("ROLE_CLIENT");
        userRepository.save(user);

        String requestBody = "{\"email\":\"khachhang_auth@gmail.com\",\"password\":\"saimatkhau\"}";

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when email does not exist")
    void testLogin_NonExistentEmail_ShouldReturnUnauthorized() throws Exception {
        String requestBody = "{\"email\":\"khongtontai@gmail.com\",\"password\":\"batkymatkhau\"}";

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}
