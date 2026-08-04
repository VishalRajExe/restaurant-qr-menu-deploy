package com.restaurantqr.modules.user.service;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank
    public String name;
    public String phone;
}
