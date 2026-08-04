package com.restaurantqr.modules.branch.service;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BranchRequest {
    @NotBlank(message = "Branch name is required")
    public String name;
    public String address;
    public String phone;
    public String openingHours;
    public Double latitude;
    public Double longitude;
}
