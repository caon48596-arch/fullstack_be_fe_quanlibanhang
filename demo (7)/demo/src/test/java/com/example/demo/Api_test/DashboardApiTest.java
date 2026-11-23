package com.example.demo.Api_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("API Test - Dashboard Service")
public class DashboardApiTest extends BaseApiTest {

    @Test
    @DisplayName("GET /api/dashboard/stats - ADMIN truy cập thống kê số liệu tổng quan thành công")
    void testGetDashboardStats_AdminSuccess() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("200")))
                .andExpect(jsonPath("$.data.totalRevenue", notNullValue()))
                .andExpect(jsonPath("$.data.newOrders", notNullValue()))
                .andExpect(jsonPath("$.data.totalProducts", notNullValue()))
                .andExpect(jsonPath("$.data.totalCustomers", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - CLIENT bị chặn khi cố gắng xem báo cáo (403 Forbidden)")
    void testGetDashboardStats_ClientForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}
