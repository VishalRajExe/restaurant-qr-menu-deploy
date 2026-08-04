package com.restaurantqr.modules.user.service;

import com.restaurantqr.common.ConflictException;
import com.restaurantqr.common.ForbiddenException;
import com.restaurantqr.common.ResourceNotFoundException;
import com.restaurantqr.modules.restaurant.service.RestaurantService;
import com.restaurantqr.modules.user.entity.User;
import com.restaurantqr.modules.user.repository.UserRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RestaurantService restaurantService;
    private final PasswordEncoder passwordEncoder;

    public Page<User> listByRestaurant(Long restaurantId, Pageable pageable) {
        return userRepository.findByRestaurantId(restaurantId, pageable);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public User createStaffUser(Long restaurantId, StaffUserRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.email)) {
            throw new ConflictException("Email already in use: " + request.email);
        }

        // Only MANAGER and STAFF roles can be created this way
        if (request.role == User.Role.SUPER_ADMIN || request.role == User.Role.RESTAURANT_OWNER) {
            throw new ForbiddenException("Cannot assign SUPER_ADMIN or RESTAURANT_OWNER role through this endpoint");
        }

        var restaurant = restaurantService.findById(restaurantId);

        var user = User.builder()
                .name(request.name)
                .email(request.email)
                .password(passwordEncoder.encode(request.temporaryPassword))
                .phone(request.phone)
                .role(request.role)
                .restaurant(restaurant)
                .status(User.Status.ACTIVE)
                .build();

        var saved = userRepository.save(user);
        log.info("Staff user created: {} role={} for restaurant={}", request.email, request.role, restaurantId);
        return saved;
    }

    /**
     * Used exclusively by the Super Admin "Create Restaurant" flow to bootstrap the
     * very first RESTAURANT_OWNER account for a brand-new restaurant. Unlike
     * {@link #createStaffUser}, this intentionally skips the role guard above,
     * since granting RESTAURANT_OWNER is precisely the point of this method —
     * it is still locked down at the controller level to SUPER_ADMIN only.
     */
    @Transactional
    public User createOwnerAccount(Long restaurantId, StaffUserRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.email)) {
            throw new ConflictException("Email already in use: " + request.email);
        }

        var restaurant = restaurantService.findById(restaurantId);

        var user = User.builder()
                .name(request.name)
                .email(request.email)
                .password(passwordEncoder.encode(request.temporaryPassword))
                .phone(request.phone)
                .role(User.Role.RESTAURANT_OWNER)
                .restaurant(restaurant)
                .status(User.Status.ACTIVE)
                .build();

        var saved = userRepository.save(user);
        log.info("Owner account created: {} for restaurant={}", request.email, restaurantId);
        return saved;
    }

    @Transactional
    public User updateProfile(Long id, Long restaurantId, UpdateProfileRequest request) {
        var user = findById(id);
        assertBelongsToRestaurant(user, restaurantId);

        user.setName(request.name);
        user.setPhone(request.phone);
        return userRepository.save(user);
    }

    @Transactional
    public void toggleStatus(Long id, Long restaurantId) {
        var user = findById(id);
        assertBelongsToRestaurant(user, restaurantId);
        user.setStatus(user.getStatus() == User.Status.ACTIVE ? User.Status.INACTIVE : User.Status.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long id, Long restaurantId) {
        var user = findById(id);
        assertBelongsToRestaurant(user, restaurantId);
        user.softDelete();
        userRepository.save(user);
    }

    private void assertBelongsToRestaurant(User user, Long restaurantId) {
        if (user.getRestaurant() == null || !user.getRestaurant().getId().equals(restaurantId)) {
            throw new ForbiddenException("User does not belong to your restaurant");
        }
    }
}
