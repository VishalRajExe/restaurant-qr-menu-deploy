package com.restaurantqr.modules.auth;

// ─── DTOs ─────────────────────────────────────────────────────────────────────

import com.restaurantqr.modules.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

class LoginRequest {
    @NotBlank @Email
    public String email;
    @NotBlank
    public String password;
}

class RegisterRequest {
    @NotBlank @Size(min = 2, max = 100)
    public String name;
    @NotBlank @Email
    public String email;
    @NotBlank @Size(min = 8, max = 64)
    public String password;
    public String phone;
    // For creating restaurant owner accounts from super admin panel
    public Long restaurantId;
}

class AuthResponse {
    public String accessToken;
    public String refreshToken;
    public String tokenType = "Bearer";
    public UserInfo user;

    static class UserInfo {
        public Long id;
        public String name;
        public String email;
        public String role;
        public Long restaurantId;
        public String restaurantName;
    }
}

class RefreshTokenRequest {
    @NotBlank
    public String refreshToken;
}

class ForgotPasswordRequest {
    @NotBlank @Email
    public String email;
}

class ResetPasswordRequest {
    @NotBlank
    public String token;
    @NotBlank @Size(min = 8)
    public String newPassword;
}

class ChangePasswordRequest {
    @NotBlank
    public String currentPassword;
    @NotBlank @Size(min = 8)
    public String newPassword;
}
