package com.restaurantqr.platform.config;

import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.repository.CategoryRepository;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.platform.modules.order.entity.Order;
import com.restaurantqr.platform.modules.order.entity.OrderItem;
import com.restaurantqr.platform.modules.order.repository.OrderRepository;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.repository.QrCodeRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.modules.ticket.entity.SupportTicket;
import com.restaurantqr.platform.modules.ticket.repository.SupportTicketRepository;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@org.springframework.core.annotation.Order(10)
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final QrCodeRepository qrCodeRepository;
    private final OrderRepository orderRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking database seed status for RestQR...");

        // 1. Seed or update Restaurant
        Restaurant restaurant = restaurantRepository.findAll().stream().findFirst().orElseGet(() -> {
            Restaurant r = Restaurant.builder()
                    .name("RestQR Gourmet Bistro")
                    .slug("main-branch")
                    .description("Artisanal fine dining paired with seamless contactless QR ordering.")
                    .phone("+1 (555) 345-6789")
                    .email("contact@restqr.com")
                    .address("123 Gourmet Blvd, New York, NY 10001")
                    .city("New York")
                    .country("USA")
                    .websiteUrl("https://restqr.com")
                    .primaryColor("#AB3500")
                    .status(Restaurant.Status.ACTIVE)
                    .verificationStatus("VERIFIED")
                    .subscriptionPlan(Restaurant.SubscriptionPlan.PROFESSIONAL)
                    .build();
            return restaurantRepository.save(r);
        });

        // 2. Seed or update Branch
        Branch branch = branchRepository.findAll().stream().findFirst().orElseGet(() -> {
            Branch b = Branch.builder()
                    .restaurant(restaurant)
                    .name("Main Manhattan Branch")
                    .address("123 Gourmet Blvd, New York, NY 10001")
                    .phone("+1 (555) 345-6789")
                    .openingHours("Mon–Sat 09:00–22:00, Sun 10:00–20:00")
                    .status(Branch.Status.ACTIVE)
                    .build();
            return branchRepository.save(b);
        });

        // 3. Seed Users
        seedUser("admin@restaurantqr.com", "Admin@12345", "Super Admin", User.Role.SUPER_ADMIN, null);
        User owner = seedUser("owner@restaurant.com", "Owner@12345", "Sarah Jenkins", User.Role.RESTAURANT_OWNER, restaurant);
        seedUser("chef@restaurant.com", "Chef@12345", "Antoine Laurent", User.Role.STAFF, restaurant);

        // 4. Seed Categories & Menu Items
        if (categoryRepository.count() == 0) {
            log.info("Seeding categories and menu items...");

            Category breakfast = categoryRepository.save(Category.builder()
                    .restaurant(restaurant)
                    .name("Breakfast & Bakery")
                    .description("Artisanal pastries, brioche toasts, and specialty coffee.")
                    .displayOrder(1)
                    .build());

            Category lunch = categoryRepository.save(Category.builder()
                    .restaurant(restaurant)
                    .name("Lunch & Gourmet Burgers")
                    .description("Prime dry-aged burgers, fresh wild seafood, and organic salads.")
                    .displayOrder(2)
                    .build());

            Category dinner = categoryRepository.save(Category.builder()
                    .restaurant(restaurant)
                    .name("Dinner & Chef Steaks")
                    .description("Signature Wagyu ribeyes, truffle pastas, and slow-cooked roasts.")
                    .displayOrder(3)
                    .build());

            Category desserts = categoryRepository.save(Category.builder()
                    .restaurant(restaurant)
                    .name("Artisanal Desserts & Wine")
                    .description("Handcrafted lava cakes, sorbets, and curated wines.")
                    .displayOrder(4)
                    .build());

            // Menu Items
            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(breakfast)
                    .name("Classic French Toast")
                    .description("Golden brioche toast served with maple syrup and fresh berries")
                    .price(new BigDecimal("8.50"))
                    .imageUrl("/img/menu-1.jpg")
                    .vegNonveg(MenuItem.FoodType.VEG)
                    .isAvailable(true)
                    .isPopular(true)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(breakfast)
                    .name("Avocado Tartine & Poached Eggs")
                    .description("Artisanal sourdough topped with smashed Hass avocado and organic eggs")
                    .price(new BigDecimal("11.00"))
                    .imageUrl("/img/menu-2.jpg")
                    .vegNonveg(MenuItem.FoodType.VEG)
                    .isAvailable(true)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(breakfast)
                    .name("Smoked Salmon Croissant")
                    .description("Flaky buttery croissant with Norwegian smoked salmon and dill cream")
                    .price(new BigDecimal("12.50"))
                    .imageUrl("/img/menu-3.jpg")
                    .vegNonveg(MenuItem.FoodType.NON_VEG)
                    .isAvailable(true)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(lunch)
                    .name("Grilled Salmon Steak")
                    .description("Wild Atlantic salmon fillet with grilled asparagus and lemon herb butter")
                    .price(new BigDecimal("24.50"))
                    .imageUrl("/img/menu-4.jpg")
                    .vegNonveg(MenuItem.FoodType.NON_VEG)
                    .isAvailable(true)
                    .isChefSpecial(true)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(lunch)
                    .name("Truffle Mushroom Burger")
                    .description("Prime beef patty with black truffle aioli, melted gruyere, and brioche")
                    .price(new BigDecimal("16.00"))
                    .imageUrl("/img/menu-5.jpg")
                    .vegNonveg(MenuItem.FoodType.NON_VEG)
                    .isAvailable(true)
                    .isPopular(true)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(dinner)
                    .name("Wagyu Beef Ribeye (250g)")
                    .description("250g grilled Wagyu ribeye with roasted garlic mash and peppercorn sauce")
                    .price(new BigDecimal("34.00"))
                    .imageUrl("/img/menu-6.jpg")
                    .vegNonveg(MenuItem.FoodType.NON_VEG)
                    .isAvailable(true)
                    .isChefSpecial(true)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(dinner)
                    .name("Black Truffle Fettuccine")
                    .description("Fresh egg fettuccine with shaved Norcia black truffles and aged parmesan")
                    .price(new BigDecimal("24.00"))
                    .imageUrl("/img/menu-7.jpg")
                    .vegNonveg(MenuItem.FoodType.VEG)
                    .isAvailable(true)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(desserts)
                    .name("Artisanal Chocolate Fondant")
                    .description("Warm Valrhona dark chocolate lava cake with Madagascar vanilla bean gelato")
                    .price(new BigDecimal("14.00"))
                    .imageUrl("/img/menu-8.jpg")
                    .vegNonveg(MenuItem.FoodType.VEG)
                    .isAvailable(true)
                    .build());
        }

        // 5. Seed QR Codes
        if (qrCodeRepository.count() == 0) {
            log.info("Seeding Table QR codes...");
            for (int i = 1; i <= 10; i++) {
                String tableNum = String.format("%02d", i);
                qrCodeRepository.save(QrCode.builder()
                        .restaurant(restaurant)
                        .branch(branch)
                        .tableNumber(tableNum)
                        .label("Table " + tableNum)
                        .token(UUID.randomUUID().toString().replace("-", ""))
                        .scanCount((long) (10 + i * 4))
                        .status(QrCode.Status.ACTIVE)
                        .build());
            }
        }

        // 6. Seed Orders
        if (orderRepository.count() == 0) {
            log.info("Seeding live restaurant orders...");

            seedOrder(restaurant, "ORD-8821", "01", "+1 (555) 234-8901", "Sarah Jenkins",
                    Order.Status.PREPARING, "Extra truffle dressing on burger.",
                    List.of(
                            createItem("Truffle Mushroom Burger", new BigDecimal("16.00"), 2, "Medium rare"),
                            createItem("Artisanal Chocolate Fondant", new BigDecimal("14.00"), 1, null)
                    ));

            seedOrder(restaurant, "ORD-8822", "04", "+1 (555) 987-6543", "Michael Chang",
                    Order.Status.ACCEPTED, "Gluten sensitivity for table.",
                    List.of(
                            createItem("Grilled Salmon Steak", new BigDecimal("24.50"), 1, null),
                            createItem("Avocado Tartine & Poached Eggs", new BigDecimal("11.00"), 1, null)
                    ));

            seedOrder(restaurant, "ORD-8823", "03", "+1 (555) 456-7890", "Emma Watson",
                    Order.Status.READY, null,
                    List.of(
                            createItem("Wagyu Beef Ribeye (250g)", new BigDecimal("34.00"), 1, "Medium")
                    ));
        }

        // 7. Seed Support Tickets
        if (supportTicketRepository.count() == 0) {
            log.info("Seeding platform support tickets...");
            supportTicketRepository.save(SupportTicket.builder()
                    .ticketNumber("TICK-1001")
                    .restaurant(restaurant)
                    .createdByUser(owner)
                    .subject("QR Scanner hardware stand setup assistance")
                    .category(SupportTicket.Category.QR_PROBLEM)
                    .priority(SupportTicket.Priority.MEDIUM)
                    .status(SupportTicket.Status.OPEN)
                    .escalationLevel(SupportTicket.EscalationLevel.LEVEL_1)
                    .build());

            supportTicketRepository.save(SupportTicket.builder()
                    .ticketNumber("TICK-1002")
                    .restaurant(restaurant)
                    .createdByUser(owner)
                    .subject("Kitchen display dual monitor setup")
                    .category(SupportTicket.Category.TECHNICAL_ISSUE)
                    .priority(SupportTicket.Priority.LOW)
                    .status(SupportTicket.Status.RESOLVED)
                    .escalationLevel(SupportTicket.EscalationLevel.LEVEL_1)
                    .build());
        }

        log.info("✅ RestQR Database seeded successfully in MySQL!");
    }

    private User seedUser(String email, String rawPassword, String name, User.Role role, Restaurant restaurant) {
        return userRepository.findByEmailAndIsDeletedFalse(email).orElseGet(() -> {
            User u = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .name(name)
                    .role(role)
                    .status(User.Status.ACTIVE)
                    .restaurant(restaurant)
                    .build();
            return userRepository.save(u);
        });
    }

    private void seedOrder(Restaurant restaurant, String orderNumber, String tableNumber,
                           String customerMobile, String customerName, Order.Status status,
                           String instructions, List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .restaurant(restaurant)
                .orderNumber(orderNumber)
                .tableNumber(tableNumber)
                .customerMobile(customerMobile)
                .customerName(customerName)
                .status(status)
                .specialInstructions(instructions)
                .totalAmount(total)
                .items(new ArrayList<>())
                .build();

        for (OrderItem item : items) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        orderRepository.save(order);
    }

    private OrderItem createItem(String name, BigDecimal price, int qty, String notes) {
        return OrderItem.builder()
                .itemName(name)
                .price(price)
                .quantity(qty)
                .subtotal(price.multiply(BigDecimal.valueOf(qty)))
                .notes(notes)
                .build();
    }
}
