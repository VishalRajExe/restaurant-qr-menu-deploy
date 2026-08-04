package com.restaurantqr.platform.modules.subscription.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.subscription.entity.Subscription;
import com.restaurantqr.platform.modules.subscription.service.SubscriptionRequest;
import com.restaurantqr.platform.modules.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final com.restaurantqr.platform.modules.subscription.service.CouponService couponService;

    /** Public — plan comparison page */
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<SubscriptionService.PlanDetails>> plans() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getPlanDetails()));
    }

    @GetMapping("/restaurants/{restaurantId}/active")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SUBSCRIPTION_VIEW')")
    public ResponseEntity<ApiResponse<Subscription>> getActive(@PathVariable Long restaurantId) {
        var sub = subscriptionService.getActiveSubscription(restaurantId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @GetMapping("/restaurants/{restaurantId}/history")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SUBSCRIPTION_VIEW')")
    public ResponseEntity<ApiResponse<List<Subscription>>> history(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getHistory(restaurantId)));
    }

    @GetMapping("/restaurants/{restaurantId}/usage-meter")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('SUBSCRIPTION_VIEW')")
    public ResponseEntity<ApiResponse<com.restaurantqr.platform.modules.subscription.dto.UsageMeterDto>> getUsageMeter(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getUsageMeter(restaurantId)));
    }

    @GetMapping("/restaurants/{restaurantId}/invoices")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SUBSCRIPTION_VIEW')")
    public ResponseEntity<ApiResponse<List<com.restaurantqr.platform.modules.subscription.dto.InvoiceDto>>> getInvoices(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getInvoices(restaurantId)));
    }

    @PostMapping("/restaurants/{restaurantId}/apply-coupon")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.restaurantqr.platform.modules.subscription.service.CouponService.CouponValidationResponse>> applyCoupon(
            @PathVariable Long restaurantId,
            @RequestParam String code,
            @RequestParam java.math.BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(couponService.validateCoupon(code, amount)));
    }

    @PatchMapping("/restaurants/{restaurantId}/auto-renew")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setAutoRenew(
            @PathVariable Long restaurantId,
            @RequestParam boolean autoRenew) {
        subscriptionService.setAutoRenew(restaurantId, autoRenew);
        return ResponseEntity.ok(ApiResponse.success("Auto-renew updated", null));
    }

    @PostMapping("/coupons")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<com.restaurantqr.platform.modules.subscription.entity.Coupon>> createCoupon(
            @Valid @RequestBody com.restaurantqr.platform.modules.subscription.service.CouponService.CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon created", couponService.createCoupon(request)));
    }

    /**
     * Called after successful payment. In production, this should be triggered
     * by a Razorpay/PayPal webhook after verifying the payment signature.
     */
    @PostMapping("/restaurants/{restaurantId}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Subscription>> activate(@PathVariable Long restaurantId,
                                                               @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subscription activated",
                subscriptionService.activate(restaurantId, request)));
    }

    @PostMapping("/restaurants/{restaurantId}/cancel")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long restaurantId) {
        subscriptionService.cancel(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Subscription cancelled", null));
    }
}

