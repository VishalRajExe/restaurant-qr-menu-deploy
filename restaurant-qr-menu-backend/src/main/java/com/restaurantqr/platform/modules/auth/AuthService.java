package com.restaurantqr.platform.modules.auth;

import com.restaurantqr.platform.common.BadRequestException;
import com.restaurantqr.platform.common.ConflictException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.config.EmailService;
import com.restaurantqr.platform.modules.auth.dto.UserRegistrationDto;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.security.JwtTokenProvider;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
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
    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
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

    // ─── Register ─────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(UserRegistrationDto request, User.Role defaultRole) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.email)) {
            throw new ConflictException("An account with this email already exists");
        }

        Restaurant restaurant = null;
        User.Role role = defaultRole;

        // Check if Chef registration with invite code
        if ("CHEF".equalsIgnoreCase(request.role) || (request.chefInviteCode != null && !request.chefInviteCode.trim().isEmpty())) {
            if (request.chefInviteCode == null || request.chefInviteCode.trim().isEmpty()) {
                throw new BadRequestException("Chef registration requires a valid restaurant invite code.");
            }
            String code = request.chefInviteCode.trim();
            restaurant = restaurantRepository.findByChefInviteCodeAndIsDeletedFalse(code)
                    .orElseThrow(() -> new BadRequestException("Invalid Chef Registration Code. Please obtain the correct code from your Restaurant Owner."));
            role = User.Role.STAFF; // Chef is kitchen staff
        } else {
            // Owner registration -> Create Restaurant and Primary Branch
            String restName = (request.restaurantName != null && !request.restaurantName.trim().isEmpty())
                    ? request.restaurantName.trim()
                    : request.name + "'s Bistro";

            String baseSlug = restName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
            if (baseSlug.isEmpty()) baseSlug = "restaurant-" + UUID.randomUUID().toString().substring(0, 4);

            String slug = baseSlug;
            int counter = 1;
            while (restaurantRepository.existsBySlugAndIsDeletedFalse(slug)) {
                slug = baseSlug + "-" + counter++;
            }

            String chefCode = "CHEF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            restaurant = Restaurant.builder()
                    .name(restName)
                    .slug(slug)
                    .phone(request.phone)
                    .email(request.email)
                    .address(request.restaurantAddress != null ? request.restaurantAddress.trim() : "Main Venue")
                    .chefInviteCode(chefCode)
                    .status(Restaurant.Status.ACTIVE)
                    .build();

            restaurant = restaurantRepository.save(restaurant);

            // Create initial default branch
            Branch branch = Branch.builder()
                    .restaurant(restaurant)
                    .name("Main Branch")
                    .address(restaurant.getAddress())
                    .phone(restaurant.getPhone())
                    .status(Branch.Status.ACTIVE)
                    .build();
            branchRepository.save(branch);

            role = User.Role.RESTAURANT_OWNER;
        }

        var user = User.builder()
                .name(request.name)
                .email(request.email)
                .password(passwordEncoder.encode(request.password))
                .phone(request.phone)
                .role(role)
                .status(User.Status.ACTIVE)
                .restaurant(restaurant)
                .build();

        userRepository.save(user);
        log.info("New user registered: {} role={} restaurantId={}", request.email, role, restaurant != null ? restaurant.getId() : null);

        var userDetails = new JwtUserDetails(user);
        return buildAuthResponse(userDetails, user);
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isRefreshToken(request.refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.extractEmail(request.refreshToken);
        var user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new BadRequestException("Account is inactive or suspended");
        }

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
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtUserDetails principal)) {
            throw new com.restaurantqr.platform.common.ForbiddenException("Authentication required");
        }

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
            info.restaurantSlug = user.getRestaurant().getSlug();
            info.chefInviteCode = user.getRestaurant().getChefInviteCode();
        }
        response.user = info;
        return response;
    }
}
