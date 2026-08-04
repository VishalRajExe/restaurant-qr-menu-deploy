package com.restaurantqr.platform.modules.restaurant.service;

import com.restaurantqr.platform.common.*;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.modules.subscription.repository.SubscriptionRepository;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository repository;
    private final BranchRepository branchRepository;
    private final MenuItemRepository menuItemRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final com.restaurantqr.platform.users.repository.UserRepository userRepository;


    public Restaurant findById(Long id) {
        Restaurant restaurant = repository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
        assertRestaurantAccess(id);
        return restaurant;
    }

    public Restaurant findBySlug(String slug) {
        return repository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + slug));
    }

    public Page<Restaurant> findAll(String search, Pageable pageable) {
        return repository.findAllActive(search, pageable);
    }

    @Transactional
    public Restaurant create(RestaurantRequest request) {
        if (repository.existsBySlugAndIsDeletedFalse(request.slug)) {
            throw new ConflictException("A restaurant with this slug already exists: " + request.slug);
        }

        var restaurant = Restaurant.builder()
                .name(request.name)
                .slug(request.slug.toLowerCase().replaceAll("[^a-z0-9-]", "-"))
                .description(request.description)
                .phone(request.phone)
                .email(request.email)
                .address(request.address)
                .city(request.city)
                .country(request.country)
                .websiteUrl(request.websiteUrl)
                .primaryColor(request.primaryColor != null ? request.primaryColor : "#FF6B35")
                .isTrial(true)
                .trialEndsAt(java.time.LocalDateTime.now().plusDays(14))
                .subscriptionPlan(Restaurant.SubscriptionPlan.STARTER)
                .build();

        return repository.save(restaurant);
    }

    @Transactional
    public Restaurant update(Long id, RestaurantRequest request) {
        var restaurant = findById(id);

        if (!restaurant.getSlug().equals(request.slug)
                && repository.existsBySlugAndIsDeletedFalse(request.slug)) {
            throw new ConflictException("Slug already in use");
        }

        restaurant.setName(request.name);
        restaurant.setSlug(request.slug);
        restaurant.setDescription(request.description);
        restaurant.setPhone(request.phone);
        restaurant.setEmail(request.email);
        restaurant.setAddress(request.address);
        restaurant.setCity(request.city);
        restaurant.setCountry(request.country);
        restaurant.setWebsiteUrl(request.websiteUrl);
        if (request.primaryColor != null) restaurant.setPrimaryColor(request.primaryColor);

        return repository.save(restaurant);
    }

    @Transactional
    public void delete(Long id) {
        var restaurant = findById(id);
        restaurant.softDelete();
        repository.save(restaurant);
        log.info("Restaurant soft-deleted: id={}", id);
    }

    // ─── Subscription Limit Guards ────────────────────────────────────────────

    public void assertBranchLimit(Long restaurantId) {
        var plan = findById(restaurantId).getSubscriptionPlan();
        if (plan == Restaurant.SubscriptionPlan.STARTER || plan == Restaurant.SubscriptionPlan.BASIC) {
            long count = branchRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
            if (count >= 1) throw new SubscriptionLimitException(
                    "Your STARTER plan allows 1 branch. Upgrade to Professional for more.");
        } else if (plan == Restaurant.SubscriptionPlan.PROFESSIONAL) {
            long count = branchRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
            if (count >= 5) throw new SubscriptionLimitException(
                    "Your PROFESSIONAL plan allows 5 branches. Upgrade to Business for more.");
        } else if (plan == Restaurant.SubscriptionPlan.BUSINESS) {
            long count = branchRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
            if (count >= 15) throw new SubscriptionLimitException(
                    "Your BUSINESS plan allows 15 branches. Upgrade to Enterprise for unlimited.");
        }
    }

    public void assertMenuItemLimit(Long restaurantId) {
        var plan = findById(restaurantId).getSubscriptionPlan();
        if (plan == Restaurant.SubscriptionPlan.STARTER || plan == Restaurant.SubscriptionPlan.BASIC) {
            long count = menuItemRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
            if (count >= 100) throw new SubscriptionLimitException(
                    "Your STARTER plan allows 100 menu items. Upgrade to Professional for unlimited.");
        }
    }

    public void assertStaffUserLimit(Long restaurantId) {
        var plan = findById(restaurantId).getSubscriptionPlan();
        long count = userRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);

        if ((plan == Restaurant.SubscriptionPlan.STARTER || plan == Restaurant.SubscriptionPlan.BASIC) && count >= 2) {
            throw new SubscriptionLimitException("Your STARTER plan allows max 2 staff users. Upgrade for more.");
        } else if (plan == Restaurant.SubscriptionPlan.PROFESSIONAL && count >= 10) {
            throw new SubscriptionLimitException("Your PROFESSIONAL plan allows max 10 staff users. Upgrade for more.");
        } else if (plan == Restaurant.SubscriptionPlan.BUSINESS && count >= 50) {
            throw new SubscriptionLimitException("Your BUSINESS plan allows max 50 staff users. Upgrade to Enterprise.");
        }
    }


    // ─── Access Control ───────────────────────────────────────────────────────

    /**
     * Ensures the currently authenticated user can access the given restaurant.
     * Allows access if the user is SUPER_ADMIN or if the restaurantId matches the user's restaurant.
     * Allows unauthenticated access (for public endpoints).
     * Throws ForbiddenException otherwise.
     */
    public void assertRestaurantAccess(Long restaurantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ForbiddenException("Authentication required to access restaurant details");
        }

        Object principal = auth.getPrincipal();
        Long userRestaurantId = null;
        boolean isSuperAdmin = false;

        if (principal instanceof JwtUserDetails userDetails) {
            userRestaurantId = userDetails.getRestaurantId();
            isSuperAdmin = "SUPER_ADMIN".equals(userDetails.getRole());
        } else if (principal instanceof User user) {
            userRestaurantId = user.getRestaurant() != null ? user.getRestaurant().getId() : null;
            isSuperAdmin = user.getRole() == User.Role.SUPER_ADMIN;
        }
        // Fallback: treat as no restaurant if principal type unknown
        if (userRestaurantId == null && !isSuperAdmin) {
            throw new ForbiddenException("Access denied: user has no restaurant association");
        }
        if (!isSuperAdmin && !restaurantId.equals(userRestaurantId)) {
            throw new ForbiddenException("Access denied to restaurant " + restaurantId);
        }
    }
}