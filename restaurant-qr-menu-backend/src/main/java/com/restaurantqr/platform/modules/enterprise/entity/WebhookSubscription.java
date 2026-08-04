package com.restaurantqr.platform.modules.enterprise.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "webhook_subscriptions",
        indexes = @Index(name = "idx_webhook_restaurant", columnList = "restaurant_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "events", nullable = false)
    private String events; // Comma-separated: ORDER_CREATED, SCAN_LOGGED, MENU_UPDATED

    @Column(name = "secret_key", nullable = false, length = 100)
    private String secretKey;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
