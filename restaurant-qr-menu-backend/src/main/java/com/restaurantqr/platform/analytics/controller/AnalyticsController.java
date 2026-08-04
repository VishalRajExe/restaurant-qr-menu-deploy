package com.restaurantqr.platform.analytics.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/restaurants/{restaurantId}/dashboard")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<ApiResponse<com.restaurantqr.platform.analytics.dto.RestaurantDashboardResponse>> dashboard(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getRestaurantDashboard(restaurantId)));
    }

    @GetMapping("/restaurants/{restaurantId}/summary")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<ApiResponse<AnalyticsService.DashboardStats>> summary(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDashboardStats(restaurantId)));
    }
}

