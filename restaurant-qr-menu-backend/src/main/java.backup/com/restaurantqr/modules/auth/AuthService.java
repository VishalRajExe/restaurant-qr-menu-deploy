package com.restaurantqr.modules.auth;

import com.restaurantqr.common.BadRequestException;
import com.restaurantqr.common.ConflictException;
import com.restaurantqr.common.ResourceNotFoundException;
import com.restaurantqr.config.EmailService;
import com.restaurantqr.modules.user.entity.User;
import com.restaurantqr.modules.user.repository.UserRepository;
import com.restaurantqr.security.JwtTokenProvider;
import com.restaurantqr.security.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // ─── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException if wrong → handled by GlobalExceptionHandler
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email, request.password)
        );

        var userDetails = (JwtUserDetails) authentication.getPrincipal();
        var user = userRepository.findByEmailAndIsDeletedFalse(request.email)
                .orElseThrow(() -> new ResourceNotFoundException("User", -1L));

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponse(userDetails, user);
    }

    // ─── Register (used by Super Admin to create owner accounts) ─────────────

    @Transactional
    public AuthResponse register(RegisterRequest request, User.Role role) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.email)) {
            throw new ConflictException("An account with this email already exists");
        }

        var user = User.builder()
                .name(request.name)
                .email(request.email)
                .password(passwordEncoder.encode(request.password))
                .phone(request.phone)
                .role(role)
                .status(User.Status.ACTIVE)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} role={}", request.email, role);

        var userDetails = new JwtUserDetails(user);
        return buildAuthResponse(userDetails, user);
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.extractEmail(request.refreshToken);
        var user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var userDetails = new JwtUserDetails(user);
        return buildAuthResponse(userDetails, user);
    }

    // ─── Forgot / Reset Password ──────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailAndIsDeletedFalse(request.email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            log.info("Password reset requested for {}", request.email);
        });
        // Always return success (don't leak if email exists)
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        var user = userRepository.findByResetTokenAndIsDeletedFalse(request.token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    // ─── Change Password ──────────────────────────────────────────────────────

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        var principal = (JwtUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        var user = userRepository.findByEmailAndIsDeletedFalse(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword));
        userRepository.save(user);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(JwtUserDetails userDetails, User user) {
        var response = new AuthResponse();
        response.accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        response.refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        var info = new AuthResponse.UserInfo();
        info.id = user.getId();
        info.name = user.getName();
        info.email = user.getEmail();
        info.role = user.getRole().name();
        if (user.getRestaurant() != null) {
            info.restaurantId = user.getRestaurant().getId();
            info.restaurantName = user.getRestaurant().getName();
        }
        response.user = info;
        return response;
    }
}
