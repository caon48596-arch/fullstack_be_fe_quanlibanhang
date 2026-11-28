package com.example.demo.service;

import com.example.demo.Repository.CouponRepository;
import com.example.demo.entity.Coupon;
import com.example.demo.exception.BusinessException;
import com.example.demo.service.contract.CouponOperations;
import com.example.demo.service.discount.CouponDiscountPolicy;
import com.example.demo.service.discount.FixedAmountCouponDiscountPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tách riêng toàn bộ nghiệp vụ quản lý, kiểm tra, trừ lượt và hoàn lượt Coupon
 * ra khỏi CouponController và OrderService (tuân thủ SRP, OCP, DIP).
 */
@Service
@Transactional
public class CouponService implements CouponOperations {

    private final CouponRepository couponRepository;
    private final List<CouponDiscountPolicy> discountPolicies;

    @Autowired
    public CouponService(CouponRepository couponRepository, List<CouponDiscountPolicy> discountPolicies) {
        this.couponRepository = couponRepository;
        this.discountPolicies = (discountPolicies != null && !discountPolicies.isEmpty())
                ? List.copyOf(discountPolicies)
                : List.of(new FixedAmountCouponDiscountPolicy());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @Override
    public Coupon createCoupon(Coupon coupon) {
        if (coupon == null || coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            throw new BusinessException("Mã giảm giá không được để trống");
        }
        String normalizedCode = coupon.getCode().trim();
        if (couponRepository.findByCode(normalizedCode).isPresent()) {
            throw new BusinessException("Mã giảm giá '" + normalizedCode + "' đã tồn tại");
        }
        coupon.setCode(normalizedCode);
        coupon.setActive(true);
        return couponRepository.save(coupon);
    }

    @Override
    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateActiveCoupon(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException("Mã giảm giá không hợp lệ");
        }
        Coupon coupon = couponRepository.findByCode(code.trim())
                .orElseThrow(() -> new BusinessException("Mã giảm giá không tồn tại"));

        if (!coupon.isActive() || coupon.getQuantity() == null || coupon.getQuantity() <= 0) {
            throw new BusinessException("Mã giảm giá đã hết lượt sử dụng hoặc không hợp lệ");
        }
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Mã giảm giá đã hết hạn sử dụng");
        }
        return coupon;
    }

    @Override
    public AppliedCoupon applyCouponForOrder(String couponCode, BigDecimal orderSubtotal, LocalDateTime now) {
        BigDecimal safeSubtotal = orderSubtotal != null ? orderSubtotal : BigDecimal.ZERO;
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return new AppliedCoupon(null, safeSubtotal, BigDecimal.ZERO);
        }

        String trimmedCode = couponCode.trim();
        Coupon coupon = couponRepository.findByCodeForUpdate(trimmedCode)
                .orElseThrow(() -> new BusinessException("Mã giảm giá không tồn tại"));

        if (!coupon.isActive() || coupon.getQuantity() == null || coupon.getQuantity() <= 0) {
            throw new BusinessException("Mã giảm giá đã hết lượt sử dụng hoặc không hợp lệ");
        }

        LocalDateTime checkTime = now != null ? now : LocalDateTime.now();
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(checkTime)) {
            throw new BusinessException("Mã giảm giá đã hết hạn sử dụng");
        }

        CouponDiscountPolicy policy = discountPolicies.stream()
                .filter(p -> p.supports(coupon))
                .findFirst()
                .orElseGet(FixedAmountCouponDiscountPolicy::new);

        BigDecimal discount = policy.calculateDiscount(coupon, safeSubtotal);
        BigDecimal discountedTotal = safeSubtotal.subtract(discount);
        if (discountedTotal.compareTo(BigDecimal.ZERO) < 0) {
            discountedTotal = BigDecimal.ZERO;
        }

        coupon.setQuantity(coupon.getQuantity() - 1);
        if (coupon.getQuantity() <= 0) {
            coupon.setActive(false);
        }
        couponRepository.save(coupon);

        return new AppliedCoupon(coupon.getCode(), discountedTotal, discount);
    }

    @Override
    public void restoreCouponIfPresent(String couponCode, LocalDateTime now) {
        if (couponCode == null || couponCode.isBlank()) {
            return;
        }
        LocalDateTime checkTime = now != null ? now : LocalDateTime.now();
        couponRepository.findByCodeForUpdate(couponCode).ifPresent(coupon -> {
            int currentQty = coupon.getQuantity() != null ? coupon.getQuantity() : 0;
            coupon.setQuantity(currentQty + 1);
            boolean notExpired = coupon.getExpirationDate() == null
                    || coupon.getExpirationDate().isAfter(checkTime);
            if (notExpired) {
                coupon.setActive(true);
            }
            couponRepository.save(coupon);
        });
    }
}
