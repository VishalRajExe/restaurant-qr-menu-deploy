package com.restaurantqr.platform.modules.menuitem.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemRequest;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;

    // ─── Public menu (Customer scans QR) ──────────────────────────────────────

    @GetMapping("/public/restaurants/{restaurantId}/menu")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getPublicMenu(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getPublicMenu(restaurantId)));
    }

    @GetMapping("/public/restaurants/{restaurantId}/menu/search")
    public ResponseEntity<ApiResponse<Page<MenuItem>>> searchMenu(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MenuItem.FoodType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(menuItemService.searchMenu(restaurantId, q, type, pageable)));
    }

    @GetMapping("/public/restaurants/{restaurantId}/menu/featured")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getFeatured(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getFeatured(restaurantId)));
    }

    // ─── Admin CRUD ───────────────────────────────────────────────────────────

    @GetMapping("/restaurants/{restaurantId}/menu-items/category/{categoryId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getByCategory(@PathVariable Long categoryId,
                                                                      @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getByCategory(categoryId, restaurantId)));
    }

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_CREATE')")
    public ResponseEntity<ApiResponse<MenuItem>> create(
            @PathVariable Long restaurantId,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created", menuItemService.create(restaurantId, request)));
    }

    @PutMapping("/restaurants/{restaurantId}/menu-items/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_EDIT')")
    public ResponseEntity<ApiResponse<MenuItem>> update(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Menu item updated", menuItemService.update(id, restaurantId, request)));
    }

    @PatchMapping("/restaurants/{restaurantId}/menu-items/{id}/availability")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN') or hasAuthority('MENU_EDIT')")
    public ResponseEntity<ApiResponse<Void>> toggleAvailability(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        menuItemService.updateAvailability(id, restaurantId, body.getOrDefault("available", true));
        return ResponseEntity.ok(ApiResponse.success("Availability updated", null));
    }

    @DeleteMapping("/restaurants/{restaurantId}/menu-items/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long restaurantId,
            @PathVariable Long id) {
        menuItemService.delete(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted", null));
    }

    @PostMapping("/restaurants/{restaurantId}/menu-items/{id}/restore")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_CREATE')")
    public ResponseEntity<ApiResponse<MenuItem>> restore(
            @PathVariable Long restaurantId,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Menu item restored", menuItemService.restore(id, restaurantId)));
    }
}

