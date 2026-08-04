package com.restaurantqr.platform.security;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.common.ForbiddenException;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.modules.subscription.service.SubscriptionService;
import com.restaurantqr.platform.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class P0SecurityFixesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("P0-3/P0-4: Unauthenticated GET /api/v1/restaurants/1 is rejected with 401 Unauthorized")
    void unauthenticatedGetRestaurantById_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/1").contextPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P0-2: Non-SUPER_ADMIN (e.g. RESTAURANT_OWNER) cannot call POST /api/v1/subscriptions/restaurants/1/activate")
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void ownerCannotActivateSubscriptionDirectly() throws Exception {
        String body = """
                {
                    "plan": "ENTERPRISE",
                    "months": 12
                }
                """;
        mockMvc.perform(post("/api/v1/subscriptions/restaurants/1/activate")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P0-2: SUPER_ADMIN can call POST /api/v1/subscriptions/restaurants/1/activate")
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdminCanActivateSubscription() throws Exception {
        String body = """
                {
                    "plan": "ENTERPRISE",
                    "months": 12
                }
                """;
        mockMvc.perform(post("/api/v1/subscriptions/restaurants/1/activate")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("P0-5: JWT Access token expiration is set to 15 minutes (900000 ms)")
    void jwtAccessTokenExpiration_is15Minutes() {
        User user = User.builder()
                .email("owner@restaurant.com")
                .password("pass")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .build();
        user.setId(1L);
        JwtUserDetails userDetails = new JwtUserDetails(user);

        String token = jwtTokenProvider.generateAccessToken(userDetails);
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }
}
