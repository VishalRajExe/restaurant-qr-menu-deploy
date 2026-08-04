package com.restaurantqr.modules.restaurant.service;

import com.restaurantqr.common.*;
import com.restaurantqr.modules.branch.repository.BranchRepository;
import com.restaurantqr.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.modules.restaurant.entity.Restaurant;
import com.restaurantqr.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.modules.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final MenuItemRepository menuItemRepository;
    private final SubscriptionRepository subscriptionRepository;

    public Restaurant findById(Long id) {
        return restaurantRepository.findById(id)
                .filter(r -> !r.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
    }

    public Restaurant findBySlug(String slug) {
        return restaurantRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + slug));
    }

    public Page<Restaurant> findAll(String search, Pageable pageable) {
        return restaurantRepository.findAllActive(search, pageable);
    }

    @Transactional
    public Restaurant create(RestaurantRequest request) {
        if (restaurantRepository.existsBySlugAndIsDeletedFalse(request.slug)) {
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
                .build();

        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public Restaurant update(Long id, RestaurantRequest request) {
        var restaurant = findById(id);

        if (!restaurant.getSlug().equals(request.slug)
                && restaurantRepository.existsBySlugAndIsDeletedFalse(request.slug)) {
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

        return restaurantRepository.save(restaurant);
    }

    @Transactional
    public void delete(Long id) {
        var restaurant = findById(id);
        restaurant.softDelete();
        restaurantRepository.save(restaurant);
        log.info("Restaurant soft-deleted: id={}", id);
    }

    // ─── Subscription Limit Guards ────────────────────────────────────────────

    public void assertBranchLimit(Long restaurantId) {
        var plan = findById(restaurantId).getSubscriptionPlan();
        if (plan == Restaurant.SubscriptionPlan.BASIC) {
            long count = branchRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
            if (count >= 1) throw new SubscriptionLimitException(
                    "Your BASIC plan allows 1 branch. Upgrade to Professional for more.");
        } else if (plan == Restaurant.SubscriptionPlan.PROFESSIONAL) {
            long count = branchRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
            if (count >= 5) throw new SubscriptionLimitException(
                    "Your PROFESSIONAL plan allows 5 branches. Upgrade to Enterprise for more.");
        }
    }

    public void assertMenuItemLimit(Long restaurantId) {
        var plan = findById(restaurantId).getSubscriptionPlan();
        if (plan == Restaurant.SubscriptionPlan.BASIC) {
            long count = menuItemRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
            if (count >= 100) throw new SubscriptionLimitException(
                    "Your BASIC plan allows 100 menu items. Upgrade to Professional for unlimited.");
        }
    }
}
