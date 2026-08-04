package com.restaurantqr.platform;

import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class RestaurantQrApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantQrApplication.class, args);
    }

    /**
     * Seeds the default Super Admin account on first boot.
     * Credentials: admin@restaurantqr.com / (password from env ADMIN_PASSWORD or generated)
     * IMPORTANT: Change this password immediately after first login!
     */
    @Bean
    CommandLineRunner seedSuperAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String superAdminEmail = "admin@restaurantqr.com";
            String envPassword = System.getenv("ADMIN_PASSWORD");
            String password = (envPassword != null && !envPassword.isBlank()) ? envPassword : "Admin@12345";

            var existingAdmin = userRepository.findByEmailAndIsDeletedFalse(superAdminEmail);
            if (existingAdmin.isEmpty()) {
                var admin = User.builder()
                        .name("Super Admin")
                        .email(superAdminEmail)
                        .password(passwordEncoder.encode(password))
                        .role(User.Role.SUPER_ADMIN)
                        .status(User.Status.ACTIVE)
                        .build();

                userRepository.save(admin);
                log.info("========================================================");
                log.info("  Super Admin seeded: {}", superAdminEmail);
                log.info("  Default password:   {}", password);
                log.info("========================================================");
            } else {
                User admin = existingAdmin.get();
                admin.setPassword(passwordEncoder.encode(password));
                userRepository.save(admin);
                log.info("Super Admin password synchronized on boot: {}", superAdminEmail);
            }
        };
    }
}
