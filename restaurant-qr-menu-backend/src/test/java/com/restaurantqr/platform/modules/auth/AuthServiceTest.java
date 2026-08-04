package com.restaurantqr.platform.modules.auth;

import com.restaurantqr.platform.common.ConflictException;
import com.restaurantqr.platform.modules.auth.dto.UserRegistrationDto;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import com.restaurantqr.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Register succeeds with valid data and unique email")
    void register_success() {
        when(userRepository.existsByEmailAndIsDeletedFalse(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        var request = new UserRegistrationDto();
        request.name     = "Test User";
        request.email    = "test@example.com";
        request.password = "Password@123";

        var response = authService.register(request, User.Role.RESTAURANT_OWNER);

        assertThat(response.accessToken).isEqualTo("access-token");
        assertThat(response.refreshToken).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register throws ConflictException when email already exists")
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmailAndIsDeletedFalse("existing@example.com")).thenReturn(true);

        var request = new UserRegistrationDto();
        request.email    = "existing@example.com";
        request.password = "Password@123";
        request.name     = "User";

        assertThatThrownBy(() -> authService.register(request, User.Role.RESTAURANT_OWNER))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Login succeeds with valid credentials")
    void login_success() {
        var jwtUser = new com.restaurantqr.platform.security.JwtUserDetails(testUser);
        var authToken = new UsernamePasswordAuthenticationToken(jwtUser, null, jwtUser.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(userRepository.findByEmailAndIsDeletedFalse(anyString())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");
        when(userRepository.save(any())).thenReturn(testUser);

        var request = new LoginRequest();
        request.email    = "test@example.com";
        request.password = "Password@123";

        var response = authService.login(request);

        assertThat(response.accessToken).isNotBlank();
        assertThat(response.user.email).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Reset password with expired token throws BadRequestException")
    void resetPassword_expiredToken_throws() {
        var user = User.builder()
                .email("test@example.com")
                .resetToken("expired-token")
                .resetTokenExpiry(java.time.LocalDateTime.now().minusHours(2))
                .build();

        when(userRepository.findByResetTokenAndIsDeletedFalse("expired-token"))
                .thenReturn(Optional.of(user));

        var request = new ResetPasswordRequest();
        request.token       = "expired-token";
        request.newPassword = "NewPassword@123";

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(com.restaurantqr.platform.common.BadRequestException.class)
                .hasMessageContaining("expired");
    }
}