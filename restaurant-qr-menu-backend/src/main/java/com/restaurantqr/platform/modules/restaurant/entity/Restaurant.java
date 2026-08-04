package com.restaurantqr.platform.modules.restaurant.entity;

import com.restaurantqr.platform.common.BaseEntity;

import com.restaurantqr.platform.modules.subscription.entity.Subscription;
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
    @Column(name = "subscription_plan", nullable = false, length = 50)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.STARTER;


    @Column(name = "trial_ends_at")
    private java.time.LocalDateTime trialEndsAt;

    @Column(name = "is_trial", nullable = false)
    @Builder.Default
    private Boolean isTrial = false;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Subscription> subscriptions = new ArrayList<>();

    public boolean isTrialActive() {
        return Boolean.TRUE.equals(isTrial) && trialEndsAt != null && java.time.LocalDateTime.now().isBefore(trialEndsAt);
    }

    public enum Status { ACTIVE, INACTIVE, SUSPENDED }

    public enum SubscriptionPlan {
        STARTER,        // 1 branch, 100 menu items, 2 staff, 1 GB storage
        BASIC,          // Legacy alias for STARTER
        PROFESSIONAL,   // 5 branches, unlimited menu items, 10 staff, 10 GB storage, custom domain
        BUSINESS,       // 15 branches, unlimited menu items, 50 staff, 50 GB storage, custom domain, advanced analytics
        ENTERPRISE      // unlimited branches, menu items, staff, storage, custom domain, API access
    }
}

