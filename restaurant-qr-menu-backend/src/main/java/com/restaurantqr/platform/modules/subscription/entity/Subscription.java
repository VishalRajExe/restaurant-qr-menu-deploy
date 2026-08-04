package com.restaurantqr.platform.modules.subscription.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
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
    private String paymentGateway;   // "RAZORPAY" | "STRIPE" | "PAYPAL"

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private Boolean autoRenew = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    public boolean isActive() {
        return status == Status.ACTIVE && !LocalDate.now().isAfter(endDate);
    }

    public enum Plan {
        STARTER,
        BASIC,
        PROFESSIONAL,
        BUSINESS,
        ENTERPRISE
    }

    public enum Status { ACTIVE, EXPIRED, CANCELLED, PENDING }
}

