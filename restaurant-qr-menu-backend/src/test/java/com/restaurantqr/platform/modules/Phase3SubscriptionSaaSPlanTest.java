package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.common.SubscriptionLimitException;
import com.restaurantqr.platform.modules.branch.service.BranchRequest;
import com.restaurantqr.platform.modules.branch.service.BranchService;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.service.CategoryRequest;
import com.restaurantqr.platform.modules.category.service.CategoryService;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemRequest;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantRequest;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.modules.subscription.dto.InvoiceDto;
import com.restaurantqr.platform.modules.subscription.dto.UsageMeterDto;
import com.restaurantqr.platform.modules.subscription.entity.Coupon;
import com.restaurantqr.platform.modules.subscription.entity.Subscription;
import com.restaurantqr.platform.modules.subscription.service.CouponService;
import com.restaurantqr.platform.modules.subscription.service.SubscriptionRequest;
import com.restaurantqr.platform.modules.subscription.service.SubscriptionService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@Transactional
class Phase3SubscriptionSaaSPlanTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CouponService couponService;

    private Restaurant testRestaurant;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        RestaurantRequest req = new RestaurantRequest();
        req.name = "Subscription SaaS Test Bistro";
        req.slug = "sub-saas-test-" + System.currentTimeMillis();
        testRestaurant = restaurantService.create(req);

        ownerUser = userRepository.save(User.builder()
                .name("Sub Owner")
                .email("subowner-" + System.currentTimeMillis() + "@test.com")
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
    @DisplayName("1. 14-Day Trial Engine: New restaurant gets 14-day free trial on creation")
    void testRestaurantTrialInitialization() {
        assertTrue(testRestaurant.getIsTrial());
        assertNotNull(testRestaurant.getTrialEndsAt());
        assertTrue(testRestaurant.isTrialActive());
        assertEquals(Restaurant.SubscriptionPlan.STARTER, testRestaurant.getSubscriptionPlan());
    }

    @Test
    @DisplayName("2. Plan Limit Guards: STARTER plan blocks 2nd branch and 101st menu item")
    void testStarterPlanLimitsEnforcement() {
        BranchRequest b1 = new BranchRequest();
        b1.name = "First Branch";
        branchService.create(testRestaurant.getId(), b1);

        BranchRequest b2 = new BranchRequest();
        b2.name = "Second Branch";
        assertThrows(SubscriptionLimitException.class, () -> branchService.create(testRestaurant.getId(), b2));

        CategoryRequest catReq = new CategoryRequest();
        catReq.name = "Main Menu";
        Category category = categoryService.create(testRestaurant.getId(), catReq);

        for (int i = 1; i <= 100; i++) {
            MenuItemRequest itemReq = new MenuItemRequest();
            itemReq.name = "Item " + i;
            itemReq.price = new BigDecimal("99.00");
            itemReq.categoryId = category.getId();
            menuItemService.create(testRestaurant.getId(), itemReq);
        }

        MenuItemRequest overflow = new MenuItemRequest();
        overflow.name = "101st Item";
        overflow.price = new BigDecimal("99.00");
        overflow.categoryId = category.getId();
        assertThrows(SubscriptionLimitException.class, () -> menuItemService.create(testRestaurant.getId(), overflow));
    }

    @Test
    @DisplayName("3. Coupon Code Engine: Validate 20% discount coupon calculation")
    void testCouponValidationAndApplication() {
        CouponService.CouponRequest couponReq = new CouponService.CouponRequest();
        couponReq.setCode("WELCOME20");
        couponReq.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        couponReq.setDiscountValue(new BigDecimal("20"));
        Coupon coupon = couponService.createCoupon(couponReq);

        assertNotNull(coupon.getId());
        assertEquals("WELCOME20", coupon.getCode());

        CouponService.CouponValidationResponse validation = couponService.validateCoupon("WELCOME20", new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("200.00"), validation.getCalculatedDiscount());
        assertEquals(new BigDecimal("800.00"), validation.getFinalAmount());
    }

    @Test
    @DisplayName("4. Subscription Activation & GST Invoice: Activate plan and retrieve GST invoice")
    void testSubscriptionActivationAndInvoice() {
        SubscriptionRequest request = new SubscriptionRequest();
        request.plan = Subscription.Plan.PROFESSIONAL;
        request.months = 1;
        request.paymentGateway = "RAZORPAY";
        request.paymentId = "pay_test_998877";

        Subscription activeSub = subscriptionService.activate(testRestaurant.getId(), request);

        assertNotNull(activeSub.getId());
        assertEquals(Subscription.Plan.PROFESSIONAL, activeSub.getPlan());
        assertEquals(Restaurant.SubscriptionPlan.PROFESSIONAL, restaurantRepository.findById(testRestaurant.getId()).orElseThrow().getSubscriptionPlan());

        List<InvoiceDto> invoices = subscriptionService.getInvoices(testRestaurant.getId());
        assertFalse(invoices.isEmpty());

        InvoiceDto invoice = invoices.get(0);
        assertNotNull(invoice.getInvoiceNumber());
        assertTrue(invoice.getInvoiceNumber().startsWith("INV-"));
        assertEquals(Subscription.Plan.PROFESSIONAL, invoice.getPlan());
        assertNotNull(invoice.getTaxAmount());
    }

    @Test
    @DisplayName("5. Usage Meter: Fetch real-time dashboard usage meter metrics")
    void testUsageMeterMetrics() {
        BranchRequest b1 = new BranchRequest();
        b1.name = "Branch 1";
        branchService.create(testRestaurant.getId(), b1);

        UsageMeterDto usageMeter = subscriptionService.getUsageMeter(testRestaurant.getId());

        assertNotNull(usageMeter);
        assertEquals(testRestaurant.getId(), usageMeter.getRestaurantId());
        assertEquals(1, usageMeter.getBranchesUsed());
        assertEquals(1, usageMeter.getBranchesLimit());
        assertEquals(100, usageMeter.getMenuItemsLimit());
        assertTrue(usageMeter.isTrial());
    }
}
