package com.restaurantqr.modules.restaurant.controller;

import com.restaurantqr.common.ApiResponse;
import com.restaurantqr.modules.restaurant.entity.Restaurant;
import com.restaurantqr.modules.restaurant.service.RestaurantRequest;
import com.restaurantqr.modules.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // Public: customer fetches restaurant by slug for the menu page
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<Restaurant>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(restaurantService.findBySlug(slug)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Restaurant>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(restaurantService.findById(id)));
    }

    // Super admin: paginated list
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<Restaurant>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(restaurantService.findAll(search, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Restaurant>> create(@Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created", restaurantService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<Restaurant>> update(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated", restaurantService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        restaurantService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant deleted", null));
    }
}
