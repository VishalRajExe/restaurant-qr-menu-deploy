// ─── DTO ──────────────────────────────────────────────────────────────────────
package com.restaurantqr.platform.modules.restaurant.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RestaurantRequest {
    @NotBlank @Size(max = 200)
    public String name;
    @NotBlank @Size(max = 100)
    public String slug;
    public String description;
    public String phone;
    public String email;
    public String address;
    public String city;
    public String country;
    public String websiteUrl;
    public String primaryColor;
}
