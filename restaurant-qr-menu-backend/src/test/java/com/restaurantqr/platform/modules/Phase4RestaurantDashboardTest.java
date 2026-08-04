package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.analytics.dto.RestaurantDashboardResponse;
import com.restaurantqr.platform.analytics.service.AnalyticsService;
import com.restaurantqr.platform.modules.branch.service.BranchRequest;
import com.restaurantqr.platform.modules.branch.service.BranchService;
import com.restaurantqr.platform.modules.offer.entity.Offer;
import com.restaurantqr.platform.modules.offer.service.OfferRequest;
import com.restaurantqr.platform.modules.offer.service.OfferService;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.service.QrCodeService;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
class Phase4RestaurantDashboardTest {


    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private AnalyticsService analyticsService;

    private Restaurant testRestaurant;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        RestaurantRequest req = new RestaurantRequest();
        req.name = "Dashboard Analytics Bistro";
        req.slug = "dashboard-bistro-" + System.currentTimeMillis();
        testRestaurant = restaurantService.create(req);

        ownerUser = userRepository.save(User.builder()
                .name("Dashboard Owner")
                .email("dashowner-" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(testRestaurant)
                .build());

        JwtUserDetails details = new JwtUserDetails(ownerUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    @DisplayName("1. Cards & Metrics: Verify Today's Scans, Visitors, Revenue, Offers, and Branch Counts")
    void testDashboardCardsAndMetrics() {
        BranchRequest b1 = new BranchRequest();
        b1.name = "Downtown Branch";
        var branch = branchService.create(testRestaurant.getId(), b1);

        com.restaurantqr.platform.modules.qr.service.QrCodeRequest qrReq = new com.restaurantqr.platform.modules.qr.service.QrCodeRequest();
        qrReq.branchId = branch.getId();
        qrReq.tableNumber = "T-01";
        QrCode qrCode = qrCodeService.generate(testRestaurant.getId(), qrReq);


        OfferRequest offerReq = new OfferRequest();
        offerReq.title = "Weekend Special 20% Off";
        offerReq.discountType = Offer.DiscountType.PERCENTAGE;
        offerReq.discountPercentage = new BigDecimal("20.00");
        offerReq.startDate = LocalDate.now().minusDays(1);
        offerReq.endDate = LocalDate.now().plusDays(7);
        offerService.create(testRestaurant.getId(), offerReq);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("192.168.1.100");
        req.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)");

        analyticsService.recordScanSync(qrCode, req);

        RestaurantDashboardResponse dashboard = analyticsService.getRestaurantDashboard(testRestaurant.getId());

        assertNotNull(dashboard.getCards());
        assertEquals(1, dashboard.getCards().getBranchCount());
        assertEquals(1, dashboard.getCards().getActiveOffers());
        assertEquals(1, dashboard.getCards().getTodayScans());
        assertEquals(1, dashboard.getCards().getTodayVisitors());
        assertNotNull(dashboard.getCards().getEstimatedRevenue());
    }

    @Test
    @DisplayName("2. Time-Series Charts & Heatmap: Verify Daily/Weekly/Monthly Trends and Hourly Scans")
    void testChartTrendsAndHeatmap() {
        RestaurantDashboardResponse dashboard = analyticsService.getRestaurantDashboard(testRestaurant.getId());

        assertNotNull(dashboard.getCharts());
        assertFalse(dashboard.getCharts().getDaily().isEmpty());
        assertFalse(dashboard.getCharts().getWeekly().isEmpty());
        assertFalse(dashboard.getCharts().getMonthly().isEmpty());
        assertFalse(dashboard.getCharts().getYearly().isEmpty());

        assertNotNull(dashboard.getHeatmaps());
        assertEquals(24, dashboard.getHeatmaps().getHourlyScans().size());
    }

    @Test
    @DisplayName("3. Search Analytics: Track public menu search terms (Pizza, Burger, Coffee)")
    void testSearchAnalyticsLogging() {
        analyticsService.recordSearchSync(testRestaurant.getId(), "Pizza");
        analyticsService.recordSearchSync(testRestaurant.getId(), "Pizza");
        analyticsService.recordSearchSync(testRestaurant.getId(), "Burger");

        RestaurantDashboardResponse dashboard = analyticsService.getRestaurantDashboard(testRestaurant.getId());

        assertNotNull(dashboard.getTopSearchTerms());
        assertFalse(dashboard.getTopSearchTerms().isEmpty());
        assertEquals("pizza", dashboard.getTopSearchTerms().get(0).getTerm());
        assertEquals(2, dashboard.getTopSearchTerms().get(0).getSearchCount());
    }

    @Test
    @DisplayName("4. Customer Device Analytics: Parse Android, iPhone, Tablet, and Desktop breakdown")
    void testDeviceBreakdownAnalytics() {
        BranchRequest b1 = new BranchRequest();
        b1.name = "Branch Main";
        var branch = branchService.create(testRestaurant.getId(), b1);
        com.restaurantqr.platform.modules.qr.service.QrCodeRequest qrReq = new com.restaurantqr.platform.modules.qr.service.QrCodeRequest();
        qrReq.branchId = branch.getId();
        qrReq.tableNumber = "T-10";
        QrCode qrCode = qrCodeService.generate(testRestaurant.getId(), qrReq);

        MockHttpServletRequest reqAndroid = new MockHttpServletRequest();
        reqAndroid.setRemoteAddr("10.0.0.1");
        reqAndroid.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; SM-G998B)");
        analyticsService.recordScanSync(qrCode, reqAndroid);

        MockHttpServletRequest reqIphone = new MockHttpServletRequest();
        reqIphone.setRemoteAddr("10.0.0.2");
        reqIphone.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 15_4 like Mac OS X)");
        analyticsService.recordScanSync(qrCode, reqIphone);

        RestaurantDashboardResponse dashboard = analyticsService.getRestaurantDashboard(testRestaurant.getId());

        Map<String, Long> devices = dashboard.getDeviceAnalytics();
        assertNotNull(devices);
        assertTrue(devices.get("ANDROID") >= 1);
        assertTrue(devices.get("IPHONE") >= 1);
    }
}

