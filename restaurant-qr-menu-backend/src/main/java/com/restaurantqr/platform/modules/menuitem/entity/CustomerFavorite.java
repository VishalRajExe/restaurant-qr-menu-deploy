package com.restaurantqr.platform.modules.menuitem.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "customer_favorites",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_item", columnNames = {"device_token", "menu_item_id"}),
        indexes = @Index(name = "idx_fav_device_restaurant", columnList = "device_token, restaurant_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerFavorite extends BaseEntity {

    @Column(name = "device_token", nullable = false, length = 150)
    private String deviceToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;
}
