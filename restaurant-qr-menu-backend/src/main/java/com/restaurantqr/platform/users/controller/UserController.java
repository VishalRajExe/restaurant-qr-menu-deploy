package com.restaurantqr.platform.users.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.service.StaffInvitationService;
import com.restaurantqr.platform.users.service.StaffUserRequest;
import com.restaurantqr.platform.users.service.UpdateProfileRequest;
import com.restaurantqr.platform.users.service.UserManagementService;

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
@RequestMapping("/restaurants/{restaurantId}/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<User>>> list(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(userManagementService.listByRestaurant(restaurantId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<User>> getById(@PathVariable Long restaurantId,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.findById(id, restaurantId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<User>> createStaff(@PathVariable Long restaurantId,
                                                          @Valid @RequestBody StaffUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff user created",
                        userManagementService.createStaffUser(restaurantId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<User>> update(@PathVariable Long restaurantId,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated",
                userManagementService.updateProfile(id, restaurantId, request)));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@PathVariable Long restaurantId,
                                                           @PathVariable Long id) {
        userManagementService.toggleStatus(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("Status toggled", null));
    }

    private final StaffInvitationService staffInvitationService;

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long restaurantId,
                                                     @PathVariable Long id) {
        userManagementService.delete(id, restaurantId);
        return ResponseEntity.ok(ApiResponse.success("User removed", null));
    }

    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<StaffInvitationService.InvitationResponse>> invite(
            @PathVariable Long restaurantId,
            @Valid @RequestBody StaffInvitationService.InviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff invitation issued",
                        staffInvitationService.createInvitation(restaurantId, request)));
    }

    @GetMapping("/invitations")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','MANAGER','SUPER_ADMIN') or hasAuthority('STAFF_MANAGE')")
    public ResponseEntity<ApiResponse<java.util.List<StaffInvitationService.InvitationResponse>>> getInvitations(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(staffInvitationService.getPendingInvitations(restaurantId)));
    }
}


