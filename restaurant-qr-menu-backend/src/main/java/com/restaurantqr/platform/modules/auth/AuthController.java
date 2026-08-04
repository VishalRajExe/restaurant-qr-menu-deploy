package com.restaurantqr.platform.modules.auth;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.auth.dto.UserRegistrationDto;
import com.restaurantqr.platform.users.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody UserRegistrationDto request) {
        // Public self-registration → STAFF role (no restaurant association)
        return ResponseEntity.ok(ApiResponse.success("Account created", authService.register(request, User.Role.STAFF)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If your email is registered, you will receive a reset link", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    private final com.restaurantqr.platform.users.service.StaffInvitationService staffInvitationService;

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/invitations/{token}")
    public ResponseEntity<ApiResponse<com.restaurantqr.platform.users.service.StaffInvitationService.InvitationResponse>> getInvitation(
            @PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(staffInvitationService.getInvitationByToken(token)));
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<ApiResponse<User>> acceptInvitation(
            @Valid @RequestBody com.restaurantqr.platform.users.service.StaffInvitationService.AcceptInvitationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Invitation accepted successfully",
                staffInvitationService.acceptInvitation(request)));
    }
}

