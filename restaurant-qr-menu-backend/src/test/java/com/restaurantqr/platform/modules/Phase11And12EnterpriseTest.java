package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.admin.entity.PlatformAnnouncement;
import com.restaurantqr.platform.modules.admin.entity.SystemSetting;
import com.restaurantqr.platform.modules.admin.service.SuperAdminSaasService;
import com.restaurantqr.platform.modules.enterprise.entity.ApiKey;
import com.restaurantqr.platform.modules.enterprise.entity.CustomDomain;
import com.restaurantqr.platform.modules.enterprise.entity.SystemBackup;
import com.restaurantqr.platform.modules.enterprise.entity.WebhookSubscription;
import com.restaurantqr.platform.modules.enterprise.service.EnterpriseService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantRequest;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@Transactional
class Phase11And12EnterpriseTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SuperAdminSaasService superAdminSaasService;

    @Autowired
    private EnterpriseService enterpriseService;

    private Restaurant testRestaurant;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        RestaurantRequest req = new RestaurantRequest();
        req.name = "Enterprise Flagship Restaurant";
        req.slug = "enterprise-flagship-" + System.currentTimeMillis();
        testRestaurant = restaurantService.create(req);

        superAdminUser = userRepository.save(User.builder()
                .name("Super Admin Founder")
                .email("founder-" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(User.Role.SUPER_ADMIN)
                .status(User.Status.ACTIVE)
                .restaurant(testRestaurant)
                .build());

        JwtUserDetails details = new JwtUserDetails(superAdminUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    @DisplayName("1. SaaS Financial Dashboard & Health: Compute MRR/ARR, tenant distribution and system health")
    void testSaasFinancialDashboardAndHealth() {
        SuperAdminSaasService.SaasFinancialDashboardDto dashboard = superAdminSaasService.getSaasFinancialDashboard();
        assertNotNull(dashboard);
        assertNotNull(dashboard.getMonthlyRecurringRevenue());
        assertNotNull(dashboard.getAnnualRecurringRevenue());
        assertTrue(dashboard.getTotalRestaurants() > 0);

        SuperAdminSaasService.SystemHealthDto health = superAdminSaasService.getSystemHealth();
        assertEquals("UP", health.getStatus());
        assertEquals("CONNECTED", health.getDatabaseStatus());
    }

    @Test
    @DisplayName("2. Platform Announcements & System Settings: Global broadcasts and feature flags")
    void testPlatformAnnouncementsAndSettings() {
        PlatformAnnouncement announcement = superAdminSaasService.createAnnouncement(
                "Scheduled Maintenance Notice",
                "System upgrade on Sunday 02:00 AM UTC.",
                "ALL"
        );
        assertNotNull(announcement.getId());

        List<PlatformAnnouncement> activeAnnouncements = superAdminSaasService.getActiveAnnouncements();
        assertFalse(activeAnnouncements.isEmpty());

        SystemSetting setting = superAdminSaasService.updateSystemSetting(
                "FEATURE_WHITE_LABEL_ENABLED", "true", "Enable custom white label domain mapping"
        );
        assertEquals("true", setting.getSettingValue());
    }

    @Test
    @DisplayName("3. Developer API Keys: Generate, query, and revoke API access keys")
    void testDeveloperApiKeys() {
        ApiKey apiKey = enterpriseService.generateApiKey(testRestaurant.getId(), "POS Integration Key", 1200);
        assertNotNull(apiKey.getId());
        assertTrue(apiKey.getKeyPrefix().startsWith("rqr_live_"));
        assertNotNull(apiKey.getHashedSecret());

        List<ApiKey> keys = enterpriseService.getApiKeys(testRestaurant.getId());
        assertEquals(1, keys.size());

        enterpriseService.revokeApiKey(apiKey.getId());
        assertTrue(enterpriseService.getApiKeys(testRestaurant.getId()).isEmpty());
    }


    @Test
    @DisplayName("4. Webhook Subscriptions: Register target webhook URL and dispatch events")
    void testWebhookSubscriptions() {
        WebhookSubscription webhook = enterpriseService.registerWebhook(
                testRestaurant.getId(),
                "https://api.mypos.com/v1/webhook",
                "ORDER_CREATED,MENU_UPDATED"
        );
        assertNotNull(webhook.getId());
        assertTrue(webhook.getSecretKey().startsWith("whsec_"));

        enterpriseService.dispatchWebhook(testRestaurant.getId(), "ORDER_CREATED", "{\"orderId\": 99}");
        assertEquals(1, enterpriseService.getWebhooks(testRestaurant.getId()).size());
    }

    @Test
    @DisplayName("5. White-Label Custom Domain, Backups & Status Page: Domain CNAME verification and backup trigger")
    void testWhiteLabelCustomDomainAndBackups() {
        CustomDomain domain = enterpriseService.configureCustomDomain(
                testRestaurant.getId(),
                "menu.gourmetbistro.com",
                "https://cdn.gourmetbistro.com/logo.png",
                ".brand-header { color: #ff5722; }"
        );
        assertNotNull(domain.getId());
        assertFalse(domain.getIsCnameVerified());

        CustomDomain verified = enterpriseService.verifyCustomDomain(testRestaurant.getId());
        assertTrue(verified.getIsCnameVerified());

        SystemBackup backup = enterpriseService.triggerBackup();
        assertNotNull(backup.getFilename());
        assertEquals("COMPLETED", backup.getStatus());

        EnterpriseService.StatusPageDto statusPage = enterpriseService.getStatusPage();
        assertEquals("ALL_SYSTEMS_OPERATIONAL", statusPage.getOperationalStatus());
    }
}
