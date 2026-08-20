package com.restaurantqr.platform.config;

import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.repository.QrCodeRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.modules.table.entity.DiningTable;
import com.restaurantqr.platform.modules.table.repository.DiningTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@org.springframework.core.annotation.Order(1)
@org.springframework.context.annotation.Profile("!test")
@RequiredArgsConstructor
public class DatabaseSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DiningTableRepository diningTableRepository;
    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final QrCodeRepository qrCodeRepository;

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
            "ALTER TABLE ticket_messages ADD COLUMN sender_role VARCHAR(50) NULL",
            "ALTER TABLE qr_codes MODIFY COLUMN branch_id BIGINT NULL"
        };

        for (String sql : ticketAlters) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.debug("Ticket alter execution notice for [{}]: {}", sql, e.getMessage());
            }
        }
        log.info("Support tickets schema alters migration executed successfully.");

        // ── Seed initial dining tables for restaurants if none exist ────────
        try {
            List<Restaurant> restaurants = restaurantRepository.findAll();
            for (Restaurant r : restaurants) {
                if (diningTableRepository.countByRestaurantId(r.getId()) == 0) {
                    log.info("Seeding initial dining tables for restaurant: {} (ID: {})", r.getName(), r.getId());
                    List<Branch> branches = branchRepository.findByRestaurantId(r.getId());
                    Branch defaultBranch = branches.isEmpty() ? null : branches.get(0);
                    if (defaultBranch == null) {
                        Branch newBranch = Branch.builder()
                                .restaurant(r)
                                .name("Main Dining Hall")
                                .address(r.getAddress() != null ? r.getAddress() : "101 Downtown Blvd")
                                .phone(r.getPhone() != null ? r.getPhone() : "+1 (555) 019-2834")
                                .status(Branch.Status.ACTIVE)
                                .build();
                        defaultBranch = branchRepository.save(newBranch);
                    }

                    int[] capacities = {2, 4, 4, 6, 4, 6, 8, 4};
                    DiningTable.Status[] initialStatuses = {
                        DiningTable.Status.OCCUPIED,
                        DiningTable.Status.AVAILABLE,
                        DiningTable.Status.RESERVED,
                        DiningTable.Status.AVAILABLE,
                        DiningTable.Status.AVAILABLE,
                        DiningTable.Status.CLEANING,
                        DiningTable.Status.AVAILABLE,
                        DiningTable.Status.AVAILABLE
                    };

                    for (int i = 1; i <= 8; i++) {
                        String tableNum = "Table " + (i < 10 ? "0" + i : i);
                        String qrToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
                        String slug = r.getSlug() != null ? r.getSlug() : String.valueOf(r.getId());
                        String tableParam = String.valueOf(i < 10 ? "0" + i : i);
                        String publicMenuUrl = "http://localhost:4200/menu/" + slug + "?table=" + URLEncoder.encode(tableParam, StandardCharsets.UTF_8);
                        String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&ecc=H&color=101828&data=" + URLEncoder.encode(publicMenuUrl, StandardCharsets.UTF_8);

                        QrCode qrCode = QrCode.builder()
                                .restaurant(r)
                                .branch(defaultBranch)
                                .tableNumber(tableNum)
                                .label(tableNum)
                                .token(qrToken)
                                .qrImageUrl(qrImageUrl)
                                .scanCount((long) (Math.random() * 40 + 10))
                                .status(QrCode.Status.ACTIVE)
                                .build();
                        qrCode = qrCodeRepository.save(qrCode);

                        DiningTable.Status st = initialStatuses[i - 1];
                        DiningTable table = DiningTable.builder()
                                .restaurant(r)
                                .branch(defaultBranch)
                                .tableNumber(tableNum)
                                .capacity(capacities[i - 1])
                                .status(st)
                                .qrCode(qrCode)
                                .build();

                        if (st == DiningTable.Status.RESERVED) {
                            table.setReservationName("Emily Watson");
                            table.setReservationPhone("+1 (555) 234-8899");
                            table.setReservationTime("7:30 PM");
                            table.setReservationGuests(2);
                            table.setReservationNotes("Window table requested for anniversary.");
                        } else if (st == DiningTable.Status.OCCUPIED) {
                            table.setActiveSessionId("SES-" + System.currentTimeMillis());
                            table.setSessionStartTime(java.time.LocalDateTime.now().minusMinutes(25));
                        }

                        diningTableRepository.save(table);
                    }
                    log.info("Successfully seeded 8 dining tables with QR codes for restaurant ID: {}", r.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Initial table seeding notice: {}", e.getMessage());
        }
    }
}
