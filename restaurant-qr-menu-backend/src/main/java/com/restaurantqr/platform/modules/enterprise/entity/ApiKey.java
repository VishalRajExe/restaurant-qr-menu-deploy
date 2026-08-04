package com.restaurantqr.platform.modules.enterprise.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys",
        indexes = {
                @Index(name = "idx_api_key_restaurant", columnList = "restaurant_id"),
                @Index(name = "idx_api_key_prefix", columnList = "key_prefix")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName;

    @Column(name = "key_prefix", nullable = false, length = 30)
    private String keyPrefix;

    @Column(name = "hashed_secret", nullable = false)
    private String hashedSecret;

    @Column(name = "rate_limit_rpm", nullable = false)
    @Builder.Default
    private Integer rateLimitRpm = 600;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
