package com.restaurantqr.modules.restaurant.entity;

import com.restaurantqr.common.BaseEntity;

import com.restaurantqr.modules.subscription.entity.Subscription;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "restaurants",
        indexes = @Index(name = "idx_restaurant_slug", columnList = "slug"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", unique = true, length = 100)
    private String slug;   // e.g. "winged-cafe" → used in public QR URLs

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "banner_url")
    private String bannerUrl;
   

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "primary_color", length = 7)
    @Builder.Default
    private String primaryColor = "#FF6B35";   // Brand color for customer UI

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.BASIC;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Subscription> subscriptions = new ArrayList<>();

    public enum Status { ACTIVE, INACTIVE, SUSPENDED }

    public enum SubscriptionPlan {
        BASIC,          // 1 branch, 100 items
        PROFESSIONAL,   // 5 branches, unlimited items
        ENTERPRISE      // unlimited everything
    }
}
