package com.restaurantqr.platform.modules.admin.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.admin.entity.PlatformAnnouncement;
import com.restaurantqr.platform.modules.admin.entity.SystemSetting;
import com.restaurantqr.platform.modules.admin.service.SuperAdminSaasService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/saas")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminSaasController {

    private final SuperAdminSaasService superAdminSaasService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<SuperAdminSaasService.SaasFinancialDashboardDto>> getSaasFinancialDashboard() {
        return ResponseEntity.ok(ApiResponse.success(superAdminSaasService.getSaasFinancialDashboard()));
    }

    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<PlatformAnnouncement>> createAnnouncement(@RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Announcement created",
                superAdminSaasService.createAnnouncement(request.title, request.message, request.targetPlan)));
    }

    @GetMapping("/announcements")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<List<PlatformAnnouncement>>> getActiveAnnouncements() {
        return ResponseEntity.ok(ApiResponse.success(superAdminSaasService.getActiveAnnouncements()));
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<SystemSetting>> updateSystemSetting(@RequestBody SettingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Setting updated",
                superAdminSaasService.updateSystemSetting(request.settingKey, request.settingValue, request.description)));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<SystemSetting>>> getSystemSettings() {
        return ResponseEntity.ok(ApiResponse.success(superAdminSaasService.getSystemSettings()));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SuperAdminSaasService.SystemHealthDto>> getSystemHealth() {
        return ResponseEntity.ok(ApiResponse.success(superAdminSaasService.getSystemHealth()));
    }

    @Data
    public static class AnnouncementRequest {
        public String title;
        public String message;
        public String targetPlan;
    }

    @Data
    public static class SettingRequest {
        public String settingKey;
        public String settingValue;
        public String description;
    }
}
