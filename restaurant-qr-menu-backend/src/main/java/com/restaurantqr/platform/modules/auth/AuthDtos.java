package com.restaurantqr.platform.modules.auth;

import com.restaurantqr.platform.users.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDtos {
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class LoginRequest {
    @NotBlank @Email
    public String email;
    @NotBlank
    public String password;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class RegisterRequest {
    @NotBlank @Size(min = 2, max = 100)
    public String name;
    @NotBlank @Email
    public String email;
    @NotBlank @Size(min = 8, max = 64)
    public String password;
    public String phone;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AuthResponse {
    public String accessToken;
    public String refreshToken;
    @Builder.Default
    public String tokenType = "Bearer";
    public UserInfo user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        public Long id;
        public String name;
        public String email;
        public String role;
        public Long restaurantId;
        public String restaurantName;
        public String restaurantSlug;
        public String chefInviteCode;
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class RefreshTokenRequest {
    @NotBlank
    public String refreshToken;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ForgotPasswordRequest {
    @NotBlank @Email
    public String email;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ResetPasswordRequest {
    @NotBlank
    public String token;
    @NotBlank @Size(min = 8)
    public String newPassword;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class ChangePasswordRequest {
    @NotBlank
    public String currentPassword;
    @NotBlank @Size(min = 8)
    public String newPassword;
}

