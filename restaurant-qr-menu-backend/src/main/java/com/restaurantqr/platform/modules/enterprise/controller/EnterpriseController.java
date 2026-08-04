package com.restaurantqr.platform.modules.enterprise.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.enterprise.entity.ApiKey;
import com.restaurantqr.platform.modules.enterprise.entity.CustomDomain;
import com.restaurantqr.platform.modules.enterprise.entity.SystemBackup;
import com.restaurantqr.platform.modules.enterprise.entity.WebhookSubscription;
import com.restaurantqr.platform.modules.enterprise.service.EnterpriseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    @PostMapping("/restaurants/{restaurantId}/api-keys")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ApiResponse<ApiKey>> generateApiKey(
            @PathVariable Long restaurantId,
            @RequestBody ApiKeyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("API key generated",
                enterpriseService.generateApiKey(restaurantId, request.keyName, request.rateLimitRpm)));
    }

    @GetMapping("/restaurants/{restaurantId}/api-keys")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ApiResponse<List<ApiKey>>> getApiKeys(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(enterpriseService.getApiKeys(restaurantId)));
    }

    @DeleteMapping("/api-keys/{keyId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(@PathVariable Long keyId) {
        enterpriseService.revokeApiKey(keyId);
        return ResponseEntity.ok(ApiResponse.success("API key revoked", null));
    }

    @PostMapping("/restaurants/{restaurantId}/webhooks")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ApiResponse<WebhookSubscription>> registerWebhook(
            @PathVariable Long restaurantId,
            @RequestBody WebhookRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Webhook registered",
                enterpriseService.registerWebhook(restaurantId, request.targetUrl, request.events)));
    }

    @GetMapping("/restaurants/{restaurantId}/webhooks")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ApiResponse<List<WebhookSubscription>>> getWebhooks(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(enterpriseService.getWebhooks(restaurantId)));
    }

    @PostMapping("/restaurants/{restaurantId}/custom-domain")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ApiResponse<CustomDomain>> configureCustomDomain(
            @PathVariable Long restaurantId,
            @RequestBody CustomDomainRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Custom domain configured",
                enterpriseService.configureCustomDomain(restaurantId, request.domain, request.whiteLabelLogo, request.customCss)));
    }

    @PostMapping("/restaurants/{restaurantId}/custom-domain/verify")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<ApiResponse<CustomDomain>> verifyCustomDomain(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Custom domain CNAME verified", enterpriseService.verifyCustomDomain(restaurantId)));
    }

    @PostMapping("/backups/trigger")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SystemBackup>> triggerBackup() {
        return ResponseEntity.ok(ApiResponse.success("System backup initiated", enterpriseService.triggerBackup()));
    }

    @GetMapping("/status-page")
    public ResponseEntity<ApiResponse<EnterpriseService.StatusPageDto>> getStatusPage() {
        return ResponseEntity.ok(ApiResponse.success(enterpriseService.getStatusPage()));
    }

    @Data
    public static class ApiKeyRequest {
        public String keyName;
        public Integer rateLimitRpm;
    }

    @Data
    public static class WebhookRequest {
        public String targetUrl;
        public String events;
    }

    @Data
    public static class CustomDomainRequest {
        public String domain;
        public String whiteLabelLogo;
        public String customCss;
    }
}
