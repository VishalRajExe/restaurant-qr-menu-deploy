package com.restaurantqr.platform.modules.branch.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.service.BranchRequest;
import com.restaurantqr.platform.modules.branch.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants/{restaurantId}/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<Branch>>> list(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(branchService.findByRestaurant(restaurantId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','STAFF','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Branch>> getById(@PathVariable Long restaurantId,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(branchService.findById(id, restaurantId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Branch>> create(@PathVariable Long restaurantId,
                                                       @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Branch created", branchService.create(restaurantId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Branch>> update(@PathVariable Long restaurantId,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Branch updated", branchService.update(id, restaurantId, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long restaurantId,
                                                     @PathVariable Long id) {
        branchService.delete(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Branch deleted", null));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Branch>> restore(@PathVariable Long restaurantId,
                                                         @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Branch restored", branchService.restore(id, restaurantId)));
    }
}

