package com.example.demo.Api_test;

import com.example.demo.Repository.CouponRepository;
import com.example.demo.entity.Coupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("API Test - Coupon Service")
public class CouponApiTest extends BaseApiTest {

    @Autowired
    private CouponRepository couponRepository;

    private Coupon sampleCoupon;

    @BeforeEach
    void setUpCoupon() {
        sampleCoupon = new Coupon();
        sampleCoupon.setCode("KM_TEST_" + System.currentTimeMillis());
        sampleCoupon.setDiscountAmount(50000.0);
        sampleCoupon.setQuantity(20);
        sampleCoupon.setActive(true);
        sampleCoupon = couponRepository.save(sampleCoupon);
    }

    @Test
    @DisplayName("POST /api/coupons - ADMIN tạo mới mã giảm giá thành công")
    void testCreateCoupon_AdminSuccess() throws Exception {
        Coupon newCoupon = new Coupon();
        newCoupon.setCode("NEWYEAR" + System.currentTimeMillis());
        newCoupon.setDiscountAmount(100000.0);
        newCoupon.setQuantity(50);

        mockMvc.perform(post("/api/coupons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCoupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.discountAmount", is(100000.0)));
    }

    @Test
    @DisplayName("POST /api/coupons - CLIENT bị chặn khi cố gắng tạo mã giảm giá (403 Forbidden)")
    void testCreateCoupon_ClientForbidden() throws Exception {
        Coupon newCoupon = new Coupon();
        newCoupon.setCode("HACK_COUPON");
        newCoupon.setDiscountAmount(999999.0);

        mockMvc.perform(post("/api/coupons")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCoupon)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/coupons - ADMIN xem danh sách tất cả mã giảm giá")
    void testGetAllCoupons_AdminSuccess() throws Exception {
        mockMvc.perform(get("/api/coupons")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/coupons - CLIENT bị chặn xem danh sách mã giảm giá (403 Forbidden)")
    void testGetAllCoupons_ClientForbidden() throws Exception {
        mockMvc.perform(get("/api/coupons")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/coupons/validate/{code} - Khách kiểm tra mã giảm giá hợp lệ (Public)")
    void testValidateCoupon_Success() throws Exception {
        mockMvc.perform(get("/api/coupons/validate/" + sampleCoupon.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(sampleCoupon.getCode())))
                .andExpect(jsonPath("$.discountAmount", is(50000.0)));
    }

    @Test
    @DisplayName("GET /api/coupons/validate/{code} - Mã giảm giá không tồn tại báo 400 Bad Request")
    void testValidateCoupon_NotFound() throws Exception {
        mockMvc.perform(get("/api/coupons/validate/MA_KHONG_TON_TAI_9999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/coupons/{id} - ADMIN xóa mã giảm giá thành công")
    void testDeleteCoupon_AdminSuccess() throws Exception {
        Coupon c = new Coupon();
        c.setCode("XOA_MA_" + System.currentTimeMillis());
        c.setDiscountAmount(20000.0);
        c.setQuantity(5);
        c.setActive(true);
        c = couponRepository.save(c);

        mockMvc.perform(delete("/api/coupons/" + c.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertTrue(couponRepository.findById(c.getId()).isEmpty());
    }
}
