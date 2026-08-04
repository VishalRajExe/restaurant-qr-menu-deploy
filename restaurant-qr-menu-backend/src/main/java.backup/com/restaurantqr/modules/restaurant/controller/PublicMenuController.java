package com.restaurantqr.modules.restaurant.controller;

import com.restaurantqr.common.ApiResponse;
import com.restaurantqr.modules.analytics.service.AnalyticsService;
import com.restaurantqr.modules.category.entity.Category;
import com.restaurantqr.modules.category.service.CategoryService;
import com.restaurantqr.modules.menuitem.entity.MenuItem;
import com.restaurantqr.modules.menuitem.service.MenuItemService;
import com.restaurantqr.modules.offer.entity.Offer;
import com.restaurantqr.modules.offer.service.OfferService;
import com.restaurantqr.modules.qr.entity.QrCode;
import com.restaurantqr.modules.qr.service.QrCodeService;
import com.restaurantqr.modules.restaurant.entity.Restaurant;
import com.restaurantqr.modules.restaurant.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The single endpoint the Angular customer menu website calls after a QR scan.
 * Returns the entire menu payload in one request to minimize round-trips.
 *
 * Flow:
 *   1. Customer scans QR → gets token
 *   2. Angular calls GET /public/menu/{token}
 *   3. This endpoint resolves token → restaurant → full menu
 *   4. Analytics scan event recorded asynchronously
 */
@RestController
@RequestMapping("/public/menu")
@RequiredArgsConstructor
public class PublicMenuController {

    private final QrCodeService qrCodeService;
    private final RestaurantService restaurantService;
    private final CategoryService categoryService;
    private final MenuItemService menuItemService;
    private final OfferService offerService;
    private final AnalyticsService analyticsService;

    /**
     * Resolve QR token → full restaurant menu.
     * GET /public/menu/{token}
     */
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<MenuPayload>> getMenuByToken(
            @PathVariable String token,
            HttpServletRequest request) {

        // 1. Resolve & validate QR code
        QrCode qrCode = qrCodeService.scan(token);
        Long restaurantId = qrCode.getRestaurant().getId();

        // 2. Fetch restaurant profile
        Restaurant restaurant = restaurantService.findById(restaurantId);

        // 3. Fetch menu data
        List<Category> categories = categoryService.findActiveByRestaurant(restaurantId);
        List<MenuItem> menuItems  = menuItemService.getPublicMenu(restaurantId);
        List<Offer> activeOffers  = offerService.getActiveOffers(restaurantId);

        // 4. Record scan event asynchronously (non-blocking)
        analyticsService.recordScan(qrCode, request);

        // 5. Build payload
        var payload = MenuPayload.builder()
                .restaurant(restaurant)
                .qrCode(qrCode)
                .categories(categories)
                .menuItems(menuItems)
                .activeOffers(activeOffers)
                .build();

        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    /**
     * Direct restaurant menu by slug (for shareable links like https://menu.yourdomain.com/r/winged-cafe)
     * GET /public/menu/restaurant/{slug}
     */
    @GetMapping("/restaurant/{slug}")
    public ResponseEntity<ApiResponse<MenuPayload>> getMenuBySlug(@PathVariable String slug) {
        Restaurant restaurant = restaurantService.findBySlug(slug);
        Long restaurantId = restaurant.getId();

        List<Category> categories = categoryService.findActiveByRestaurant(restaurantId);
        List<MenuItem> menuItems  = menuItemService.getPublicMenu(restaurantId);
        List<Offer> activeOffers  = offerService.getActiveOffers(restaurantId);

        var payload = MenuPayload.builder()
                .restaurant(restaurant)
                .categories(categories)
                .menuItems(menuItems)
                .activeOffers(activeOffers)
                .build();

        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    // ─── Response Payload ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class MenuPayload {
        private Restaurant restaurant;
        private QrCode qrCode;           // null when accessed via slug
        private List<Category> categories;
        private List<MenuItem> menuItems;
        private List<Offer> activeOffers;
    }
}
