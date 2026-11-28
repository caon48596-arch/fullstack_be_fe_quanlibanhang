package com.example.demo.service.discount;

import com.example.demo.entity.Coupon;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Chính sách mặc định: Giảm số tiền cố định (Fixed Amount), không vượt quá tổng tiền đơn hàng.
 */
@Component
@Order(100)
public class FixedAmountCouponDiscountPolicy implements CouponDiscountPolicy {

    @Override
    public boolean supports(Coupon coupon) {
        return coupon != null;
    }

    @Override
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderSubtotal) {
        if (coupon == null || orderSubtotal == null || orderSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rawDiscount = coupon.getDiscountAmount() != null ? coupon.getDiscountAmount() : BigDecimal.ZERO;
        if (rawDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return rawDiscount.min(orderSubtotal);
    }
}
