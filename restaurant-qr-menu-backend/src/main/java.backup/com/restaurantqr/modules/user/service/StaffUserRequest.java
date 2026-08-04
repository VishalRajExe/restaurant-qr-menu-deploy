package com.restaurantqr.modules.user.service;

import com.restaurantqr.modules.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffUserRequest {
    @NotBlank
    public String name;
    @NotBlank @Email
    public String email;
    @NotBlank @Size(min = 8)
    public String temporaryPassword;
    public String phone;
    @NotNull
    public User.Role role; // MANAGER or STAFF only
}
