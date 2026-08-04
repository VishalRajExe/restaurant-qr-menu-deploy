package com.restaurantqr.platform.modules.category.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.config.CloudinaryUploadService;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.service.CategoryRequest;
import com.restaurantqr.platform.modules.category.service.CategoryService;
import com.restaurantqr.platform.modules.category.service.ReorderItem;
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
@RequestMapping("/restaurants/{restaurantId}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final CloudinaryUploadService cloudinaryUploadService;

    // Public — customer menu page calls this
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Category>>> listActive(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findActiveByRestaurant(restaurantId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Category>>> list(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findByRestaurant(restaurantId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Category>> getById(@PathVariable Long restaurantId,
                                                          @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.findById(id, restaurantId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Category>> create(@PathVariable Long restaurantId,
                                                         @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created", categoryService.create(restaurantId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Category>> update(@PathVariable Long restaurantId,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category updated",
                categoryService.update(id, restaurantId, request)));
    }

    // Drag-and-drop reorder: PUT /restaurants/{id}/categories/reorder
    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorder(@PathVariable Long restaurantId,
                                                      @RequestBody List<ReorderItem> items) {
        categoryService.reorder(restaurantId, items);
        return ResponseEntity.ok(ApiResponse.success("Categories reordered", null));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long restaurantId,
                                                           @PathVariable Long id) {
        categoryService.toggleStatus(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Status toggled", null));
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @PathVariable Long restaurantId,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        String url = cloudinaryUploadService.uploadImage(file, "categories/" + restaurantId);
        categoryService.updateImage(id, restaurantId, url);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", Map.of("url", url)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('CATEGORY_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long restaurantId,
                                                     @PathVariable Long id) {
        categoryService.delete(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('CATEGORY_MANAGE')")
    public ResponseEntity<ApiResponse<Category>> restore(@PathVariable Long restaurantId,
                                                           @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Category restored", categoryService.restore(id, restaurantId)));
    }
}

