package com.example.demo.service.contract;

import com.example.demo.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Hợp đồng quản lý và áp dụng Mã giảm giá (tách biệt khỏi OrderService & CouponController theo SRP/ISP/DIP).
 */
public interface CouponOperations {

    record AppliedCoupon(String code, BigDecimal discountedTotal, BigDecimal discountAmount) {}

    List<Coupon> getAllCoupons();

    Coupon createCoupon(Coupon coupon);

    void deleteCoupon(Long id);

    Coupon validateActiveCoupon(String code);

    AppliedCoupon applyCouponForOrder(String couponCode, BigDecimal orderSubtotal, LocalDateTime now);

    void restoreCouponIfPresent(String couponCode, LocalDateTime now);
}
