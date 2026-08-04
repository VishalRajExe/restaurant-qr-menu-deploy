package com.restaurantqr.modules.analytics.controller;

import com.restaurantqr.common.ApiResponse;
import com.restaurantqr.modules.analytics.service.AnalyticsService;
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
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsService.DashboardStats>> dashboard(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDashboardStats(restaurantId)));
    }
}
