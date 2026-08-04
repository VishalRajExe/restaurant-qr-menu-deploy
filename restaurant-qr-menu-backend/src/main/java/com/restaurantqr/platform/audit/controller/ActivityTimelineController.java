package com.restaurantqr.platform.audit.controller;

import com.restaurantqr.platform.audit.entity.AuditLog;
import com.restaurantqr.platform.audit.service.AuditLogService;
import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants/{restaurantId}")
@RequiredArgsConstructor
public class ActivityTimelineController {

    private final AuditLogService auditLogService;
    private final RestaurantService restaurantService;

    @GetMapping("/activity-timeline")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'MANAGER') or hasAuthority('ANALYTICS_VIEW')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getActivityTimeline(
            @PathVariable Long restaurantId) {
        restaurantService.assertRestaurantAccess(restaurantId);
        List<AuditLog> timeline = auditLogService.getActivityTimeline(restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Activity timeline retrieved successfully", timeline));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER') or hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        restaurantService.assertRestaurantAccess(restaurantId);
        Page<AuditLog> auditLogs = auditLogService.getAuditLogs(restaurantId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", auditLogs));
    }
}
