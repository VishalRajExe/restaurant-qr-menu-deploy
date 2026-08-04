package com.restaurantqr.platform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@org.springframework.context.annotation.Profile("!test")
@RequiredArgsConstructor
public class DatabaseSchemaInitializer implements CommandLineRunner {


    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Executing automatic schema compatibility check on MySQL database...");
            jdbcTemplate.execute("ALTER TABLE restaurants MODIFY COLUMN subscription_plan VARCHAR(100) NOT NULL DEFAULT 'STARTER'");
            log.info("Successfully updated restaurants.subscription_plan column to VARCHAR(100).");
        } catch (Exception e) {
            log.warn("Automatic schema alter skipped or already updated: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE support_tickets MODIFY COLUMN message TEXT NULL DEFAULT NULL");
            log.info("Successfully updated support_tickets.message column to NULLABLE.");
        } catch (Exception e) {
            log.warn("Automatic support_tickets.message alter skipped: {}", e.getMessage());
        }

    }
}
