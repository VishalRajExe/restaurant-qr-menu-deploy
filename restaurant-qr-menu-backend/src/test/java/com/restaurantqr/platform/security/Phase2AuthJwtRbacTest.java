package com.restaurantqr.platform.security;

import com.restaurantqr.platform.RestaurantQrApplication;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase2AuthJwtRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    private User activeOwner;
    private User activeStaff;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        activeOwner = User.builder()
                .email("owner@example.com")
                .password("encoded")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .build();
        activeOwner.setId(1L);

        activeStaff = User.builder()
                .email("staff@example.com")
                .password("encoded")
                .role(User.Role.STAFF)
                .status(User.Status.ACTIVE)
                .build();
        activeStaff.setId(2L);

        suspendedUser = User.builder()
                .email("suspended@example.com")
                .password("encoded")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.SUSPENDED)
                .build();
        suspendedUser.setId(3L);

        when(userRepository.findByEmailAndIsDeletedFalse("owner@example.com")).thenReturn(Optional.of(activeOwner));
        when(userRepository.findByEmailAndIsDeletedFalse("staff@example.com")).thenReturn(Optional.of(activeStaff));
        when(userRepository.findByEmailAndIsDeletedFalse("suspended@example.com")).thenReturn(Optional.of(suspendedUser));
    }

    @Test
    @DisplayName("No JWT → protected API returns 401 Unauthorized")
    void noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/1").contextPath("/api/v1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("Invalid JWT → protected API returns 401 Unauthorized")
    void invalidJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/1")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer invalid-jwt-token-xyz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Refresh Token passed as Access Token → protected API returns 401 Unauthorized")
    void refreshTokenAsAccessToken_returns401() throws Exception {
        String refreshToken = jwtTokenProvider.generateRefreshToken(new JwtUserDetails(activeOwner));
        mockMvc.perform(get("/api/v1/restaurants/1")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Lower role (STAFF/OWNER) → SUPER_ADMIN endpoint returns 403 Forbidden")
    void lowerRoleAccessSuperAdmin_returns403() throws Exception {
        String ownerToken = jwtTokenProvider.generateAccessToken(new JwtUserDetails(activeOwner));
        mockMvc.perform(get("/api/v1/super-admin/stats")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied: you don't have permission for this action"));
    }

    @Test
    @DisplayName("Suspended user JWT token → protected API returns 401 Unauthorized")
    void suspendedUserJwt_returns401() throws Exception {
        String suspendedToken = jwtTokenProvider.generateAccessToken(new JwtUserDetails(suspendedUser));
        mockMvc.perform(get("/api/v1/restaurants/1")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + suspendedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Self-registration assigns STAFF role and prevents privilege escalation")
    void selfRegistration_assignsStaffRole() throws Exception {
        when(userRepository.existsByEmailAndIsDeletedFalse("newuser@example.com")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        String registerBody = """
                {
                    "name": "New User",
                    "email": "newuser@example.com",
                    "password": "Password@123",
                    "role": "SUPER_ADMIN"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("STAFF"));
    }
}
