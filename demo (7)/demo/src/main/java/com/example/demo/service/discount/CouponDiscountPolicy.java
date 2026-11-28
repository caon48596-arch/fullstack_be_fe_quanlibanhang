package com.example.demo.service.discount;

import com.example.demo.entity.Coupon;

import java.math.BigDecimal;

/**
 * Strategy tính số tiền giảm giá cho Coupon (tuân thủ OCP).
 * Khi nâng cấp thêm mã giảm theo phần trăm (%), giảm tối đa, hoặc theo hạng thành viên,
 * chỉ cần tạo class mới implement interface này mà không phải sửa CouponService hay OrderService.
 */
public interface CouponDiscountPolicy {

    boolean supports(Coupon coupon);

    BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderSubtotal);
}
