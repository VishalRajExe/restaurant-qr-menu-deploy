package com.restaurantqr;

import com.restaurantqr.modules.user.entity.User;
import com.restaurantqr.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class RestaurantQrApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantQrApplication.class, args);
    }

    /**
     * Seeds the default Super Admin account on first boot.
     * Credentials: admin@restaurantqr.com / Admin@12345
     * IMPORTANT: Change this password immediately after first login!
     */
    @Bean
    CommandLineRunner seedSuperAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String superAdminEmail = "admin@restaurantqr.com";

            if (!userRepository.existsByEmailAndIsDeletedFalse(superAdminEmail)) {
                var admin = User.builder()
                        .name("Super Admin")
                        .email(superAdminEmail)
                        .password(passwordEncoder.encode("Admin@12345"))
                        .role(User.Role.SUPER_ADMIN)
                        .status(User.Status.ACTIVE)
                        .build();

                userRepository.save(admin);
                log.warn("========================================================");
                log.warn("  Super Admin seeded: {}", superAdminEmail);
                log.warn("  Default password:   Admin@12345");
                log.warn("  CHANGE THIS PASSWORD IMMEDIATELY AFTER FIRST LOGIN!");
                log.warn("========================================================");
            } else {
                log.info("Super Admin already exists — skipping seed.");
            }
        };
    }
}
