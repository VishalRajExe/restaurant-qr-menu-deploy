package com.restaurantqr.platform.modules.enterprise.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "custom_domains",
        indexes = @Index(name = "idx_domain_restaurant", columnList = "restaurant_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomDomain extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false, unique = true)
    private Restaurant restaurant;

    @Column(name = "custom_domain", nullable = false, unique = true)
    private String customDomain;

    @Column(name = "cname_target", nullable = false)
    @Builder.Default
    private String cnameTarget = "cname.restaurantqr.com";

    @Column(name = "is_cname_verified", nullable = false)
    @Builder.Default
    private Boolean isCnameVerified = false;

    @Column(name = "white_label_logo", length = 500)
    private String whiteLabelLogo;

    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;
}
