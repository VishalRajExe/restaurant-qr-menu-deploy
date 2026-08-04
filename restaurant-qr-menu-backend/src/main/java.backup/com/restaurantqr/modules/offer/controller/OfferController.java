package com.restaurantqr.modules.offer.controller;

import com.restaurantqr.common.ApiResponse;
import com.restaurantqr.config.CloudinaryUploadService;
import com.restaurantqr.modules.offer.entity.Offer;
import com.restaurantqr.modules.offer.service.OfferRequest;
import com.restaurantqr.modules.offer.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;
    private final CloudinaryUploadService cloudinaryUploadService;

    // Public — customer menu page shows active deals
    @GetMapping("/public/restaurants/{restaurantId}/offers")
    public ResponseEntity<ApiResponse<List<Offer>>> getActiveOffers(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(offerService.getActiveOffers(restaurantId)));
    }

    @GetMapping("/restaurants/{restaurantId}/offers")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Offer>>> list(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(offerService.getAllByRestaurant(restaurantId)));
    }

    @PostMapping("/restaurants/{restaurantId}/offers")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Offer>> create(@PathVariable Long restaurantId,
                                                      @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Offer created", offerService.create(restaurantId, request)));
    }

    @PutMapping("/restaurants/{restaurantId}/offers/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Offer>> update(@PathVariable Long restaurantId,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody OfferRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Offer updated", offerService.update(id, restaurantId, request)));
    }

    @PostMapping("/restaurants/{restaurantId}/offers/{id}/banner")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadBanner(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        String url = cloudinaryUploadService.uploadImage(file, "offers/" + restaurantId);
        offerService.updateBanner(id, restaurantId, url);
        return ResponseEntity.ok(ApiResponse.success("Banner uploaded", Map.of("url", url)));
    }

    @DeleteMapping("/restaurants/{restaurantId}/offers/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long restaurantId,
                                                     @PathVariable Long id) {
        offerService.delete(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Offer deleted", null));
    }
}
