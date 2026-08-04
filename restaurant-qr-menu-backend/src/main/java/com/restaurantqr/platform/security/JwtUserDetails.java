package com.restaurantqr.platform.security;

import com.restaurantqr.platform.users.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class JwtUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String password;
    private final String role;
    private final Long restaurantId;
    private final boolean active;
    private final boolean suspended;

    public JwtUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole().name();
        this.restaurantId = user.getRestaurant() != null ? user.getRestaurant().getId() : null;
        this.active = user.getStatus() == User.Status.ACTIVE;
        this.suspended = user.getStatus() == User.Status.SUSPENDED;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        try {
            User.Role userRole = User.Role.valueOf(role);
            for (com.restaurantqr.platform.users.entity.Permission permission : userRole.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.name()));
            }
        } catch (Exception ignored) {
        }
        return authorities;
    }


    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return !suspended; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
}
