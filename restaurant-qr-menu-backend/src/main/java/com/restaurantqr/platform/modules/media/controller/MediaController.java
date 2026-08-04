package com.restaurantqr.platform.modules.media.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.media.entity.MediaAsset;
import com.restaurantqr.platform.modules.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/restaurants/{restaurantId}/upload")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_EDIT')")
    public ResponseEntity<ApiResponse<MediaAsset>> upload(
            @PathVariable Long restaurantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", mediaService.upload(restaurantId, file, folder)));
    }

    @PostMapping("/restaurants/{restaurantId}/upload-multiple")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_EDIT')")
    public ResponseEntity<ApiResponse<List<MediaAsset>>> uploadMultiple(
            @PathVariable Long restaurantId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", required = false) String folder) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Images uploaded", mediaService.uploadMultiple(restaurantId, files, folder)));
    }

    @GetMapping("/restaurants/{restaurantId}/gallery")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_EDIT')")
    public ResponseEntity<ApiResponse<List<MediaAsset>>> getGallery(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(mediaService.getRestaurantGallery(restaurantId)));
    }

    @PostMapping("/restaurants/{restaurantId}/assets/{assetId}/crop")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_EDIT')")
    public ResponseEntity<ApiResponse<MediaAsset>> crop(
            @PathVariable Long restaurantId,
            @PathVariable Long assetId,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int width,
            @RequestParam int height) throws IOException {
        return ResponseEntity.ok(ApiResponse.success("Image cropped", mediaService.cropAsset(restaurantId, assetId, x, y, width, height)));
    }

    @GetMapping("/restaurants/{restaurantId}/assets/{assetId}/cdn-url")
    public ResponseEntity<ApiResponse<String>> getCdnUrl(
            @PathVariable Long restaurantId,
            @PathVariable Long assetId,
            @RequestParam(defaultValue = "500") int width,
            @RequestParam(defaultValue = "500") int height,
            @RequestParam(defaultValue = "fill") String cropMode) {
        return ResponseEntity.ok(ApiResponse.success(mediaService.getCdnUrl(restaurantId, assetId, width, height, cropMode)));
    }

    @DeleteMapping("/restaurants/{restaurantId}/assets/{assetId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('MENU_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable Long restaurantId,
            @PathVariable Long assetId) throws IOException {
        mediaService.deleteAsset(restaurantId, assetId);
        return ResponseEntity.ok(ApiResponse.success("Media asset deleted", null));
    }
}
