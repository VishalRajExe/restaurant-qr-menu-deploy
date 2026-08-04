package com.restaurantqr.platform.modules.subscription.service;

import com.restaurantqr.platform.common.BadRequestException;
import com.restaurantqr.platform.common.ConflictException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.subscription.entity.Coupon;
import com.restaurantqr.platform.modules.subscription.repository.CouponRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Data
    public static class CouponRequest {
        private String code;
        private Coupon.DiscountType discountType;
        private BigDecimal discountValue;
        private Integer maxUsage;
        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    public static class CouponValidationResponse {
        private String code;
        private Coupon.DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal calculatedDiscount;
        private BigDecimal finalAmount;
    }

    @Transactional
    public Coupon createCoupon(CouponRequest request) {
        String code = request.getCode().toUpperCase().trim();
        if (couponRepository.existsByCode(code)) {
            throw new ConflictException("Coupon code " + code + " already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxUsage(request.getMaxUsage() != null ? request.getMaxUsage() : 1000)
                .expiresAt(request.getExpiresAt())
                .status(Coupon.Status.ACTIVE)
                .build();

        return couponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(String code, BigDecimal originalAmount) {
        String formattedCode = code.toUpperCase().trim();
        Coupon coupon = couponRepository.findByCodeAndIsDeletedFalse(formattedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + formattedCode));

        if (!coupon.isValid()) {
            throw new BadRequestException("Coupon " + formattedCode + " is expired, disabled, or has reached max usage");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discount = originalAmount.multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else if (coupon.getDiscountType() == Coupon.DiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        }

        if (discount.compareTo(originalAmount) > 0) {
            discount = originalAmount;
        }

        BigDecimal finalAmount = originalAmount.subtract(discount);

        return CouponValidationResponse.builder()
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .calculatedDiscount(discount)
                .finalAmount(finalAmount)
                .build();
    }

    @Transactional
    public void incrementUsage(String code) {
        String formattedCode = code.toUpperCase().trim();
        couponRepository.findByCodeAndIsDeletedFalse(formattedCode).ifPresent(c -> {
            c.setTimesUsed(c.getTimesUsed() + 1);
            if (c.getMaxUsage() != null && c.getTimesUsed() >= c.getMaxUsage()) {
                c.setStatus(Coupon.Status.EXPIRED);
            }
            couponRepository.save(c);
        });
    }

    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }
}
