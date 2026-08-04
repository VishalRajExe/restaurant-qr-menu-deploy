package com.restaurantqr.modules.subscription.controller;

import com.restaurantqr.common.ApiResponse;
import com.restaurantqr.modules.subscription.entity.Subscription;
import com.restaurantqr.modules.subscription.service.SubscriptionRequest;
import com.restaurantqr.modules.subscription.service.SubscriptionService;
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

    /** Public — plan comparison page */
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<SubscriptionService.PlanDetails>> plans() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getPlanDetails()));
    }

    @GetMapping("/restaurants/{restaurantId}/active")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Subscription>> getActive(@PathVariable Long restaurantId) {
        var sub = subscriptionService.getActiveSubscription(restaurantId).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    @GetMapping("/restaurants/{restaurantId}/history")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Subscription>>> history(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(subscriptionService.getHistory(restaurantId)));
    }

    /**
     * Called after successful payment. In production, this should be triggered
     * by a Razorpay/PayPal webhook after verifying the payment signature.
     */
    @PostMapping("/restaurants/{restaurantId}/activate")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
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
