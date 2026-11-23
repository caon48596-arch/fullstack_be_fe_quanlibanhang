package com.example.demo.Api_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API Test - System Health Check Service")
public class HealthCheckApiTest extends BaseApiTest {

    @Test
    @DisplayName("GET /api/v1/system/health - Trạng thái hệ thống UP, DB connected (Public)")
    void testSystemHealthCheck_Success() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("qlbanhang-api")))
                .andExpect(jsonPath("$.database", is("Connected")));
    }
}
