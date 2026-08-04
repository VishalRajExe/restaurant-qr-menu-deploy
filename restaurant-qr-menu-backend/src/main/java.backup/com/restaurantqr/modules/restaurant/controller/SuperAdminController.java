package com.restaurantqr.modules.restaurant.controller;

import com.restaurantqr.common.ApiResponse;
import com.restaurantqr.modules.restaurant.entity.Restaurant;
import com.restaurantqr.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.modules.subscription.repository.SubscriptionRepository;
import com.restaurantqr.modules.user.entity.User;
import com.restaurantqr.modules.user.repository.UserRepository;
import com.restaurantqr.modules.user.service.StaffUserRequest;
import com.restaurantqr.modules.user.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminController {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserManagementService userManagementService;

    /** Platform dashboard — key metrics */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> platformStats() {
        long totalRestaurants = restaurantRepository.countByIsDeletedFalse();
        long totalUsers = userRepository.count();
        long activeSubs = subscriptionRepository.findExpiringSoon(
                LocalDate.now(), LocalDate.now().plusDays(9999)).size(); // rough count

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "totalRestaurants", totalRestaurants,
                "totalUsers", totalUsers,
                "activeSubs", activeSubs
        )));
    }

    /** List all restaurants with search + pagination */
    @GetMapping("/restaurants")
    public ResponseEntity<ApiResponse<Page<Restaurant>>> listRestaurants(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                restaurantRepository.findAllActive(search, pageable)));
    }

    /** Suspend / un-suspend a restaurant */
    @PatchMapping("/restaurants/{id}/status")
    public ResponseEntity<ApiResponse<Void>> setRestaurantStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        restaurantRepository.findById(id).ifPresent(r -> {
            r.setStatus(Restaurant.Status.valueOf(body.get("status").toUpperCase()));
            restaurantRepository.save(r);
        });
        return ResponseEntity.ok(ApiResponse.success("Restaurant status updated", null));
    }

    /** List all platform users */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<User>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(userRepository.findAll(pageable)));
    }

    /** Create a Restaurant Owner account and attach to a restaurant */
    @PostMapping("/restaurants/{restaurantId}/owner")
    public ResponseEntity<ApiResponse<User>> createOwner(
            @PathVariable Long restaurantId,
            @Valid @RequestBody StaffUserRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Owner account created",
                        userManagementService.createOwnerAccount(restaurantId, request)));
    }

    /** Subscriptions expiring within 7 days — for proactive support */
    @GetMapping("/subscriptions/expiring-soon")
    public ResponseEntity<ApiResponse<Object>> expiringSoon() {
        var expiring = subscriptionRepository.findExpiringSoon(
                LocalDate.now(), LocalDate.now().plusDays(7));
        return ResponseEntity.ok(ApiResponse.success(expiring));
    }
}
