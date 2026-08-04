package com.restaurantqr.platform.modules.subscription.entity;

import com.restaurantqr.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"),
        indexes = @Index(name = "idx_coupon_code", columnList = "code"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType; // PERCENTAGE | FLAT

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_usage")
    @Builder.Default
    private Integer maxUsage = 1000;

    @Column(name = "times_used", nullable = false)
    @Builder.Default
    private Integer timesUsed = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public boolean isValid() {
        if (status != Status.ACTIVE || Boolean.TRUE.equals(getIsDeleted())) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return maxUsage == null || timesUsed < maxUsage;
    }

    public enum DiscountType {
        PERCENTAGE,
        FLAT
    }

    public enum Status {
        ACTIVE,
        EXPIRED,
        DISABLED
    }
}
