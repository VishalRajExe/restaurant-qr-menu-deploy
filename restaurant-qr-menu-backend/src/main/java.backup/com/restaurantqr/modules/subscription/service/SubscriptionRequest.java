package com.restaurantqr.modules.subscription.service;

import com.restaurantqr.modules.subscription.entity.Subscription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRequest {
    @NotNull(message = "Plan is required")
    public Subscription.Plan plan;
    public Integer months;          // default 1
    public String paymentId;        // Razorpay/PayPal order ID
    public String paymentGateway;   // "RAZORPAY" | "PAYPAL"
}
