package com.example.demo.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.Repository.CouponRepository;
import com.example.demo.entity.Coupon;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired
    private CouponRepository couponRepository;

    @GetMapping
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        coupon.setActive(true);
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate/{code}")
    public ResponseEntity<Coupon> validateCoupon(@PathVariable String code) {
        Optional<Coupon> couponOpt = couponRepository.findByCode(code);
        if (couponOpt.isPresent()) {
            Coupon coupon = couponOpt.get();
            if (coupon.isActive() && coupon.getQuantity() > 0) {
                return ResponseEntity.ok(coupon);
            }
        }
        return ResponseEntity.status(400).build();
    }
}
