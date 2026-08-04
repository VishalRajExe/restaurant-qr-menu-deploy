package com.restaurantqr.platform.modules.menuitem.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.config.CloudinaryUploadService;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemService;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class ImageUploadController {

    private final CloudinaryUploadService cloudinaryUploadService;
    private final MenuItemService menuItemService;
    private final RestaurantRepository restaurantRepository;

    // Upload menu item image
    @PostMapping("/menu-items/{restaurantId}/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadMenuItemImage(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String url = cloudinaryUploadService.uploadImage(file, "menus/" + restaurantId);
        menuItemService.updateImageUrl(itemId, restaurantId, url);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", Map.of("url", url)));
    }

    // Upload restaurant logo
    @PostMapping("/restaurants/{restaurantId}/logo")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadLogo(
            @PathVariable Long restaurantId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String url = cloudinaryUploadService.uploadImage(file, "logos");
        // Update restaurant logo
        restaurantRepository.findById(restaurantId).ifPresent(r -> {
            r.setLogoUrl(url);
            restaurantRepository.save(r);
        });
        return ResponseEntity.ok(ApiResponse.success("Logo uploaded", Map.of("url", url)));
    }

    // Upload restaurant banner
    @PostMapping("/restaurants/{restaurantId}/banner")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBanner(
            @PathVariable Long restaurantId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String url = cloudinaryUploadService.uploadImage(file, "banners");
        restaurantRepository.findById(restaurantId).ifPresent(r -> {
            r.setBannerUrl(url);
            restaurantRepository.save(r);
        });
        return ResponseEntity.ok(ApiResponse.success("Banner uploaded", Map.of("url", url)));
    }
}
