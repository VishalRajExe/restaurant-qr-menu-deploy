package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.modules.subscription.entity.Subscription;
import com.restaurantqr.platform.modules.subscription.repository.SubscriptionRepository;
import com.restaurantqr.platform.security.JwtTokenProvider;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase6SubscriptionsAndSuperAdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RestaurantRepository restaurantRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private SubscriptionRepository subscriptionRepository;

    @MockBean
    private BranchRepository branchRepository;

    private Restaurant basicRestaurant;

    private User superAdmin;
    private User owner;

    private String tokenSuperAdmin;
    private String tokenOwner;

    @BeforeEach
    void setUp() {
        basicRestaurant = Restaurant.builder()
                .name("Basic Rest")
                .slug("basic-rest")
                .subscriptionPlan(Restaurant.SubscriptionPlan.BASIC)
                .status(Restaurant.Status.ACTIVE)
                .build();
        basicRestaurant.setId(10L);

        superAdmin = User.builder()
                .email("admin@platform.com")
                .password("enc")
                .role(User.Role.SUPER_ADMIN)
                .status(User.Status.ACTIVE)
                .build();
        superAdmin.setId(1L);

        owner = User.builder()
                .email("owner@basic.com")
                .password("enc")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(basicRestaurant)
                .build();
        owner.setId(10L);

        tokenSuperAdmin = jwtTokenProvider.generateAccessToken(new JwtUserDetails(superAdmin));
        tokenOwner = jwtTokenProvider.generateAccessToken(new JwtUserDetails(owner));

        when(userRepository.findByEmailAndIsDeletedFalse("admin@platform.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.findByEmailAndIsDeletedFalse("owner@basic.com")).thenReturn(Optional.of(owner));
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(basicRestaurant));
    }

    @Test
    @DisplayName("Super Admin Stats: Accessible by SUPER_ADMIN, denied for RESTAURANT_OWNER")
    void superAdminStats_authorizationCheck() throws Exception {
        mockMvc.perform(get("/api/v1/super-admin/stats")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/super-admin/stats")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Restaurant Status Patch: SUPER_ADMIN can suspend a restaurant")
    void suspendRestaurant_success() throws Exception {
        String body = """
                {
                    "status": "SUSPENDED"
                }
                """;
        mockMvc.perform(patch("/api/v1/super-admin/restaurants/10/status")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restaurant status updated"));
    }

    @Test
    @DisplayName("Subscription Activation Bypass: Direct activation by RESTAURANT_OWNER returns 403 Forbidden")
    void directActivationByOwner_forbidden() throws Exception {
        String body = """
                {
                    "plan": "ENTERPRISE",
                    "months": 12,
                    "paymentId": "pay_fake_123",
                    "paymentGateway": "RAZORPAY"
                }
                """;
        mockMvc.perform(post("/api/v1/subscriptions/restaurants/10/activate")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Subscription Plan Limit Enforcement: BASIC plan limit (1 branch) blocks 2nd branch creation")
    void planLimit_branchLimitExceeded_throws402() throws Exception {
        when(branchRepository.countByRestaurantIdAndIsDeletedFalse(10L)).thenReturn(1L); // Already at limit

        String body = """
                {
                    "name": "Second Branch St",
                    "address": "456 Market St"
                }
                """;
        mockMvc.perform(post("/api/v1/restaurants/10/branches")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    @DisplayName("Subscription Cancellation: Owner can cancel active subscription")
    void cancelSubscription_success() throws Exception {
        var sub = Subscription.builder()
                .restaurant(basicRestaurant)
                .plan(Subscription.Plan.BASIC)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(25))
                .amountPaid(new BigDecimal("999"))
                .status(Subscription.Status.ACTIVE)
                .build();
        sub.setId(100L);

        when(subscriptionRepository.findActiveSubscription(10L, LocalDate.now()))
                .thenReturn(Optional.of(sub));

        mockMvc.perform(post("/api/v1/subscriptions/restaurants/10/cancel")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subscription cancelled"));
    }
}
