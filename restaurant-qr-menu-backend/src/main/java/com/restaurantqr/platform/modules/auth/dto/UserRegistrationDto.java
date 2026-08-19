package com.restaurantqr.platform.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration.
 * Handles Owner registration with new Restaurant creation and Chef registration via Invite Code.
 */
public class UserRegistrationDto {

    @NotBlank
    @Size(min = 2, max = 100)
    public String name;

    @NotBlank
    @Email
    public String email;

    @NotBlank
    @Size(min = 8, max = 64)
    public String password;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    public String phone;

    public String restaurantName;
    public String restaurantAddress;
    public String role;             // "OWNER" or "CHEF"
    public String chefInviteCode;   // Required if registering as Chef
}