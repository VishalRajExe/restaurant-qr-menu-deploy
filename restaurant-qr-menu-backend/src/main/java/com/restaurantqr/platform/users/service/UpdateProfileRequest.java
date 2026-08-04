package com.restaurantqr.platform.users.service;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank
    public String name;
    public String phone;
}
