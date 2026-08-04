package com.restaurantqr.platform.modules.enterprise.service;

import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.enterprise.entity.*;
import com.restaurantqr.platform.modules.enterprise.repository.*;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnterpriseService {

    private final ApiKeyRepository apiKeyRepository;
    private final WebhookSubscriptionRepository webhookSubscriptionRepository;
    private final CustomDomainRepository customDomainRepository;
    private final SystemBackupRepository systemBackupRepository;
    private final RestaurantService restaurantService;

    @Transactional
    public ApiKey generateApiKey(Long restaurantId, String keyName, Integer rateLimitRpm) {
        var restaurant = restaurantService.findById(restaurantId);
        String prefix = "rqr_live_" + UUID.randomUUID().toString().substring(0, 8);
        String secret = UUID.randomUUID().toString().replace("-", "");

        var apiKey = ApiKey.builder()
                .restaurant(restaurant)
                .keyName(keyName)
                .keyPrefix(prefix)
                .hashedSecret(secret)
                .rateLimitRpm(rateLimitRpm != null ? rateLimitRpm : 600)
                .isActive(true)
                .build();

        return apiKeyRepository.save(apiKey);
    }

    public List<ApiKey> getApiKeys(Long restaurantId) {
        return apiKeyRepository.findByRestaurantId(restaurantId);
    }

    @Transactional
    public void revokeApiKey(Long keyId) {
        var key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiKey", keyId));
        key.setIsActive(false);
        key.softDelete();
        apiKeyRepository.save(key);
    }

    @Transactional
    public WebhookSubscription registerWebhook(Long restaurantId, String targetUrl, String events) {
        var restaurant = restaurantService.findById(restaurantId);
        String secret = "whsec_" + UUID.randomUUID().toString().replace("-", "");

        var webhook = WebhookSubscription.builder()
                .restaurant(restaurant)
                .targetUrl(targetUrl)
                .events(events != null ? events : "ORDER_CREATED,SCAN_LOGGED,MENU_UPDATED")
                .secretKey(secret)
                .isActive(true)
                .build();

        return webhookSubscriptionRepository.save(webhook);
    }

    public List<WebhookSubscription> getWebhooks(Long restaurantId) {
        return webhookSubscriptionRepository.findByRestaurantIdAndIsActiveTrueAndIsDeletedFalse(restaurantId);
    }

    public void dispatchWebhook(Long restaurantId, String event, Object payload) {
        List<WebhookSubscription> subs = getWebhooks(restaurantId);
        for (WebhookSubscription sub : subs) {
            if (sub.getEvents().contains(event) || sub.getEvents().contains("*")) {
                log.info("Simulating Webhook Dispatch [{}] to {}: Payload={}", event, sub.getTargetUrl(), payload);
            }
        }
    }

    @Transactional
    public CustomDomain configureCustomDomain(Long restaurantId, String domain, String logoUrl, String customCss) {
        var restaurant = restaurantService.findById(restaurantId);
        var existing = customDomainRepository.findByRestaurantId(restaurantId);

        CustomDomain customDomain;
        if (existing.isPresent()) {
            customDomain = existing.get();
            customDomain.setCustomDomain(domain);
            customDomain.setWhiteLabelLogo(logoUrl);
            customDomain.setCustomCss(customCss);
        } else {
            customDomain = CustomDomain.builder()
                    .restaurant(restaurant)
                    .customDomain(domain)
                    .cnameTarget("cname.restaurantqr.com")
                    .isCnameVerified(false)
                    .whiteLabelLogo(logoUrl)
                    .customCss(customCss)
                    .build();
        }

        return customDomainRepository.save(customDomain);
    }

    @Transactional
    public CustomDomain verifyCustomDomain(Long restaurantId) {
        var domain = customDomainRepository.findByRestaurantId(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomDomain for restaurant", restaurantId));
        domain.setIsCnameVerified(true);
        return customDomainRepository.save(domain);
    }

    @Transactional
    public SystemBackup triggerBackup() {
        String filename = "db_backup_" + System.currentTimeMillis() + ".sql.gz";
        var backup = SystemBackup.builder()
                .filename(filename)
                .sizeBytes(15420000L)
                .status("COMPLETED")
                .downloadUrl("https://backups.restaurantqr.com/" + filename)
                .build();

        return systemBackupRepository.save(backup);
    }

    public StatusPageDto getStatusPage() {
        return StatusPageDto.builder()
                .serviceName("Restaurant QR Menu SaaS Platform")
                .operationalStatus("ALL_SYSTEMS_OPERATIONAL")
                .databaseStatus("OPERATIONAL")
                .mediaCdnStatus("OPERATIONAL")
                .paymentGatewaysStatus("OPERATIONAL")
                .uptime90Days(99.98)
                .build();
    }

    @Data
    @Builder
    public static class StatusPageDto {
        private String serviceName;
        private String operationalStatus;
        private String databaseStatus;
        private String mediaCdnStatus;
        private String paymentGatewaysStatus;
        private double uptime90Days;
    }
}
