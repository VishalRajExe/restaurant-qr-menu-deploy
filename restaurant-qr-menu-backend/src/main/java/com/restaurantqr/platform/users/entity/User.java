package com.restaurantqr.platform.users.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "email"),
        indexes = @Index(name = "idx_users_email", columnList = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    // NEVER serialize the password hash back to clients. This field is only ever
    // populated internally (via PasswordEncoder) and read by Spring Security —
    // it must never appear in a JSON response.
    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "last_login_at")
    private java.time.LocalDateTime lastLoginAt;

    // Password reset — internal use only, never exposed via the API
    @JsonIgnore
    @Column(name = "reset_token")
    private String resetToken;

    @JsonIgnore
    @Column(name = "reset_token_expiry")
    private java.time.LocalDateTime resetTokenExpiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    public enum Role {
        SUPER_ADMIN,
        RESTAURANT_OWNER,
        MANAGER,
        STAFF;

        public java.util.Set<Permission> getPermissions() {
            switch (this) {
                case SUPER_ADMIN:
                case RESTAURANT_OWNER:
                    return java.util.EnumSet.allOf(Permission.class);
                case MANAGER:
                    return java.util.EnumSet.of(
                            Permission.MENU_CREATE,
                            Permission.MENU_EDIT,
                            Permission.MENU_DELETE,
                            Permission.CATEGORY_MANAGE,
                            Permission.QR_MANAGE,
                            Permission.ANALYTICS_VIEW,
                            Permission.REPORT_EXPORT,
                            Permission.SETTINGS_EDIT
                    );
                case STAFF:
                    return java.util.EnumSet.of(
                            Permission.MENU_EDIT,
                            Permission.QR_MANAGE
                    );
                default:
                    return java.util.Collections.emptySet();
            }
        }
    }


    public enum Status {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}
