package com.restaurantqr.modules.subscription.entity;

import com.restaurantqr.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restaurantqr.modules.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "subscriptions",
        indexes = @Index(name = "idx_sub_restaurant", columnList = "restaurant_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription extends BaseEntity {

    // Restaurant.subscriptions is the side that gets serialized; ignoring the
    // back-reference here prevents infinite recursion (Restaurant -> subscriptions
    // -> Subscription -> restaurant -> subscriptions -> ...) which otherwise
    // throws a StackOverflowError as soon as a restaurant has any subscription.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private Plan plan;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "currency", length = 5)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "payment_id", length = 100)
    private String paymentId;   // Razorpay/PayPal order ID

    @Column(name = "payment_gateway", length = 30)
    private String paymentGateway;   // "RAZORPAY" | "PAYPAL"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public boolean isActive() {
        return status == Status.ACTIVE && !LocalDate.now().isAfter(endDate);
    }

    public enum Plan {
        BASIC,          // 1 branch, 100 items
        PROFESSIONAL,   // 5 branches, unlimited items
        ENTERPRISE      // unlimited
    }

    public enum Status { ACTIVE, EXPIRED, CANCELLED, PENDING }
}
