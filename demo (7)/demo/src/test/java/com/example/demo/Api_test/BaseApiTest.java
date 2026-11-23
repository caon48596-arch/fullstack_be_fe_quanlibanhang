package com.example.demo.Api_test;

import com.example.demo.Repository.UserRepository;
import com.example.demo.entity.User;
import com.example.demo.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class BaseApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtUtil jwtUtil;

    protected String adminToken;
    protected String clientToken;
    protected User adminUser;
    protected User clientUser;

    @BeforeEach
    public void baseSetUp() {
        adminUser = userRepository.findByEmail("admin_api@test.com").orElseGet(() -> {
            User user = new User();
            user.setEmail("admin_api@test.com");
            user.setPassword(passwordEncoder.encode("admin123"));
            user.setRole("ROLE_ADMIN");
            user.setUsername("Admin Test");
            user.setPhone("0987654321");
            user.setDob("1990-01-01");
            return userRepository.save(user);
        });
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), "ROLE_ADMIN");

        clientUser = userRepository.findByEmail("client_api@test.com").orElseGet(() -> {
            User user = new User();
            user.setEmail("client_api@test.com");
            user.setPassword(passwordEncoder.encode("client123"));
            user.setRole("ROLE_CLIENT");
            user.setUsername("Client Test");
            user.setPhone("0912345678");
            user.setDob("2000-01-01");
            return userRepository.save(user);
        });
        clientToken = jwtUtil.generateToken(clientUser.getEmail(), "ROLE_CLIENT");
    }
}
