package com.restaurantqr.platform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@org.springframework.core.annotation.Order(1)
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

        try {
            jdbcTemplate.execute("ALTER TABLE restaurants ADD COLUMN verification_status VARCHAR(50) DEFAULT 'PENDING_VERIFICATION'");
            log.info("Successfully added verification_status column on restaurants table.");
        } catch (Exception e) {
            log.warn("verification_status column already exists: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("UPDATE restaurants SET verification_status = 'PENDING_VERIFICATION' WHERE verification_status IS NULL");
            log.info("Set default PENDING_VERIFICATION for existing NULL verification_status rows.");
        } catch (Exception e) {
            log.warn("Update NULL verification_status skipped: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE notifications MODIFY COLUMN event_type VARCHAR(100) NOT NULL");
            log.info("Successfully updated notifications.event_type column to VARCHAR(100).");
        } catch (Exception e) {
            log.warn("Automatic notifications.event_type alter skipped: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE notifications MODIFY COLUMN restaurant_id BIGINT NULL");
            log.info("Successfully updated notifications.restaurant_id column to NULLABLE.");
        } catch (Exception e) {
            log.warn("notifications.restaurant_id alter skipped: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE restaurants ADD COLUMN chef_invite_code VARCHAR(50) NULL");
            log.info("Successfully added chef_invite_code column on restaurants table.");
        } catch (Exception e) {
            log.warn("chef_invite_code column already exists: {}", e.getMessage());
        }

        String[] ticketAlters = {
            "ALTER TABLE support_tickets MODIFY COLUMN created_by_user_id BIGINT NULL",
            "ALTER TABLE support_tickets MODIFY COLUMN category VARCHAR(100) NOT NULL",
            "ALTER TABLE support_tickets MODIFY COLUMN status VARCHAR(100) NOT NULL",
            "ALTER TABLE support_tickets MODIFY COLUMN priority VARCHAR(100) NOT NULL",
            "ALTER TABLE support_tickets ADD COLUMN customer_name VARCHAR(100) NULL",
            "ALTER TABLE support_tickets ADD COLUMN customer_mobile VARCHAR(30) NULL",
            "ALTER TABLE support_tickets ADD COLUMN customer_email VARCHAR(150) NULL",
            "ALTER TABLE ticket_messages MODIFY COLUMN sender_user_id BIGINT NULL",
            "ALTER TABLE ticket_messages ADD COLUMN sender_name VARCHAR(100) NULL",
            "ALTER TABLE ticket_messages ADD COLUMN sender_role VARCHAR(50) NULL"
        };

        for (String sql : ticketAlters) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.debug("Ticket alter execution notice for [{}]: {}", sql, e.getMessage());
            }
        }
        log.info("Support tickets schema alters migration executed successfully.");
    }
}
