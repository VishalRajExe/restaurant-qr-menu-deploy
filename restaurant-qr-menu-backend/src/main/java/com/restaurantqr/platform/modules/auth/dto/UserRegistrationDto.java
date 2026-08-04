package com.restaurantqr.platform.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration.
 * Used for public self-registration endpoint.
 * Does NOT contain role or tenant association fields to prevent privilege escalation.
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

    public String phone;
}