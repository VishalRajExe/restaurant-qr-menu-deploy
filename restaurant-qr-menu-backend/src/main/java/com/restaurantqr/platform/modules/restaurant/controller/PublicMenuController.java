package com.restaurantqr.platform.modules.restaurant.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.analytics.service.AnalyticsService;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.service.CategoryService;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemService;
import com.restaurantqr.platform.modules.offer.entity.Offer;
import com.restaurantqr.platform.modules.offer.service.OfferService;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.service.QrCodeService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // ─── Resolve QR token → full restaurant menu.
    // GET /public/menu/{token}
    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<MenuPayload>> getMenuByToken(
            @PathVariable String token,
            HttpServletRequest request) {

        // 1. Try resolving as QR code token first
        Restaurant restaurant = null;
        QrCode qrCode = null;
        try {
            qrCode = qrCodeService.scan(token);
            restaurant = qrCode.getRestaurant();
            analyticsService.recordScan(qrCode, request);
        } catch (Exception e) {
            // Fallback: Try resolving as restaurant slug
            try {
                restaurant = restaurantService.findBySlug(token);
            } catch (Exception ex) {
                throw new com.restaurantqr.platform.common.ResourceNotFoundException("QR code or Restaurant not found for identifier: " + token);
            }
        }

        if (restaurant == null) {
            throw new com.restaurantqr.platform.common.ResourceNotFoundException("QR code or Restaurant not found for identifier: " + token);
        }


        Long restaurantId = restaurant.getId();

        // 3. Fetch menu data
        List<Category> categories = categoryService.findActiveByRestaurant(restaurantId);
        List<MenuItem> menuItems = menuItemService.getPublicMenu(restaurantId);
        List<Offer> activeOffers = offerService.getActiveOffers(restaurantId);

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


    // ─── Direct restaurant menu by slug (for shareable links like https://menu.yourdomain.com/r/winged-cafe)
    // GET /public/menu/restaurant/{slug}
    @GetMapping("/restaurant/{slug}")
    public ResponseEntity<ApiResponse<MenuPayload>> getMenuBySlug(@PathVariable String slug) {
        Restaurant restaurant = restaurantService.findBySlug(slug);
        if (restaurant.getStatus() != Restaurant.Status.ACTIVE) {
            throw new com.restaurantqr.platform.common.ResourceNotFoundException("Restaurant not found or inactive");
        }
        Long restaurantId = restaurant.getId();

        List<Category> categories = categoryService.findActiveByRestaurant(restaurantId);
        List<MenuItem> menuItems = menuItemService.getPublicMenu(restaurantId);
        List<Offer> activeOffers = offerService.getActiveOffers(restaurantId);

        var payload = MenuPayload.builder()
                .restaurant(restaurant)
                .qrCode(null)           // null when accessed via slug
                .categories(categories)
                .menuItems(menuItems)
                .activeOffers(activeOffers)
                .build();

        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @GetMapping("/restaurants/{restaurantId}/search")
    public ResponseEntity<ApiResponse<List<MenuItem>>> searchPublicMenu(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MenuItem.FoodType vegNonveg) {
        analyticsService.recordSearch(restaurantId, q);
        return ResponseEntity.ok(ApiResponse.success(menuItemService.searchPublicMenu(restaurantId, q, vegNonveg)));
    }

    @GetMapping("/restaurants/{restaurantId}/recommended")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getRecommended(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getRecommended(restaurantId)));
    }

    @GetMapping("/restaurants/{restaurantId}/recently-added")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getRecentlyAdded(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getRecentlyAdded(restaurantId)));
    }

    @GetMapping("/restaurants/{restaurantId}/combos")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getCombos(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getCombos(restaurantId)));
    }

    @GetMapping("/restaurants/{restaurantId}/items/{itemId}/related")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getRelatedItems(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getRelatedItems(restaurantId, itemId)));
    }

    @PostMapping("/restaurants/{restaurantId}/favorites/toggle")
    public ResponseEntity<ApiResponse<Boolean>> toggleFavorite(
            @PathVariable Long restaurantId,
            @RequestParam String deviceToken,
            @RequestParam Long menuItemId) {
        boolean isFav = menuItemService.toggleFavorite(deviceToken, restaurantId, menuItemId);
        return ResponseEntity.ok(ApiResponse.success(isFav ? "Added to favorites" : "Removed from favorites", isFav));
    }

    @GetMapping("/restaurants/{restaurantId}/favorites")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getFavorites(
            @PathVariable Long restaurantId,
            @RequestParam String deviceToken) {
        return ResponseEntity.ok(ApiResponse.success(menuItemService.getFavorites(deviceToken, restaurantId)));
    }



    // ─── Response Payload
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