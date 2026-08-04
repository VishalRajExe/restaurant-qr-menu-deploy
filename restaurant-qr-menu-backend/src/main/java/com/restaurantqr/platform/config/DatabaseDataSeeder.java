package com.restaurantqr.platform.config;

import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.repository.CategoryRepository;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.repository.QrCodeRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DatabaseDataSeeder implements CommandLineRunner {

    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final QrCodeRepository qrCodeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking MySQL database initialization & seeding default data...");

        // 1. Seed Restaurant
        Restaurant restaurant = restaurantRepository.findBySlugAndIsDeletedFalse("gourmet-bistro")
                .orElseGet(() -> {
                    Restaurant newRest = Restaurant.builder()
                            .name("Gourmet Bistro")
                            .slug("gourmet-bistro")
                            .description("Exquisite culinary experiences & artisanal dining.")
                            .email("owner@restaurantqr.com")
                            .phone("+1 555 019 2831")
                            .address("104 Broadway Suite A")
                            .city("New York")
                            .country("USA")
                            .primaryColor("#fc6011")
                            .status(Restaurant.Status.ACTIVE)
                            .subscriptionPlan(Restaurant.SubscriptionPlan.PROFESSIONAL)
                            .build();
                    log.info("Seeding default restaurant Gourmet Bistro into MySQL database...");
                    return restaurantRepository.save(newRest);
                });

        // 2. Seed Branch
        List<Branch> branches = branchRepository.findByRestaurantId(restaurant.getId());
        Branch mainBranch;
        if (branches.isEmpty()) {
            mainBranch = Branch.builder()
                    .restaurant(restaurant)
                    .name("Main Flagship Branch")
                    .address("104 Broadway Suite A, New York")
                    .phone("+1 555 019 2831")
                    .openingHours("Mon-Sun 10:00 - 23:00")
                    .status(Branch.Status.ACTIVE)
                    .build();
            mainBranch = branchRepository.save(mainBranch);
            log.info("Seeding default branch into MySQL database...");
        } else {
            mainBranch = branches.get(0);
        }

        // 3. Seed Users (Owner & Chef)
        userRepository.findByEmailAndIsDeletedFalse("owner@restaurantqr.com").orElseGet(() -> {
            User owner = User.builder()
                    .name("John Owner")
                    .email("owner@restaurantqr.com")
                    .password(passwordEncoder.encode("Owner@12345"))
                    .role(User.Role.RESTAURANT_OWNER)
                    .restaurant(restaurant)
                    .status(User.Status.ACTIVE)
                    .build();
            log.info("Seeding default Owner user into MySQL...");
            return userRepository.save(owner);
        });

        userRepository.findByEmailAndIsDeletedFalse("chef@restaurantqr.com").orElseGet(() -> {
            User chef = User.builder()
                    .name("Chef Laurent")
                    .email("chef@restaurantqr.com")
                    .password(passwordEncoder.encode("Chef@12345"))
                    .role(User.Role.STAFF)
                    .restaurant(restaurant)
                    .status(User.Status.ACTIVE)
                    .build();
            log.info("Seeding default Chef user into MySQL...");
            return userRepository.save(chef);
        });

        // 4. Seed Categories
        List<Category> existingCats = categoryRepository.findActiveByRestaurantId(restaurant.getId());
        Category catAppetizers, catMain, catDesserts, catDrinks;

        if (existingCats.isEmpty()) {
            catAppetizers = categoryRepository.save(Category.builder()
                    .restaurant(restaurant)
                    .name("Appetizers")
                    .description("Starters and artisanal small bites")
                    .displayOrder(1)
                    .status(Category.Status.ACTIVE)
                    .build());

            catMain = categoryRepository.save(Category.builder()
                    .restaurant(restaurant)
                    .name("Main Course")
                    .description("Chef special main platters")
                    .displayOrder(2)
                    .status(Category.Status.ACTIVE)
                    .build());

            catDesserts = categoryRepository.save(Category.builder()
                    .restaurant(restaurant)
                    .name("Desserts")
                    .description("Sweet delicacies & pastries")
                    .displayOrder(3)
                    .status(Category.Status.ACTIVE)
                    .build());

            catDrinks = categoryRepository.save(Category.builder()
                    .restaurant(restaurant)
                    .name("Beverages")
                    .description("Refreshing drinks & artisanal coffees")
                    .displayOrder(4)
                    .status(Category.Status.ACTIVE)
                    .build());

            log.info("Seeding default categories into MySQL database...");
        } else {
            catAppetizers = existingCats.get(0);
            catMain = existingCats.size() > 1 ? existingCats.get(1) : catAppetizers;
            catDesserts = existingCats.size() > 2 ? existingCats.get(2) : catAppetizers;
            catDrinks = existingCats.size() > 3 ? existingCats.get(3) : catAppetizers;
        }

        // 5. Seed Menu Items
        List<MenuItem> existingItems = menuItemRepository.findActiveByRestaurantId(restaurant.getId());
        if (existingItems.isEmpty()) {
            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(catMain)
                    .name("Truffle Mushroom Burger")
                    .description("Artisanal beef patty with black truffle mayonnaise, Swiss cheese, and caramelized onions.")
                    .price(new BigDecimal("18.50"))
                    .imageUrl("https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=600&q=80")
                    .vegNonveg(MenuItem.FoodType.NON_VEG)
                    .isAvailable(true)
                    .isPopular(true)
                    .displayOrder(1)
                    .status(MenuItem.Status.ACTIVE)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(catAppetizers)
                    .name("Artisanal French Fries")
                    .description("Hand-cut golden potatoes seasoned with sea salt and fresh rosemary.")
                    .price(new BigDecimal("8.00"))
                    .imageUrl("https://images.unsplash.com/photo-1576107232684-1279f3908594?auto=format&fit=crop&w=600&q=80")
                    .vegNonveg(MenuItem.FoodType.VEG)
                    .isAvailable(true)
                    .isPopular(true)
                    .displayOrder(2)
                    .status(MenuItem.Status.ACTIVE)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(catMain)
                    .name("Glazed Salmon Fillet")
                    .description("Pan-seared Atlantic salmon with maple mustard glaze served on asparagus.")
                    .price(new BigDecimal("24.50"))
                    .imageUrl("https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?auto=format&fit=crop&w=600&q=80")
                    .vegNonveg(MenuItem.FoodType.NON_VEG)
                    .isPopular(true)
                    .isAvailable(true)
                    .displayOrder(3)
                    .status(MenuItem.Status.ACTIVE)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(catDesserts)
                    .name("Molten Lava Chocolate Cake")
                    .description("Warm Belgian chocolate cake with a molten chocolate center and vanilla gelato.")
                    .price(new BigDecimal("11.00"))
                    .imageUrl("https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=600&q=80")
                    .vegNonveg(MenuItem.FoodType.VEG)
                    .isPopular(true)
                    .isAvailable(true)
                    .displayOrder(4)
                    .status(MenuItem.Status.ACTIVE)
                    .build());

            menuItemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(catDrinks)
                    .name("Artisanal Iced Espresso")
                    .description("Double shot specialty roast espresso shaken over ice with organic oat milk.")
                    .price(new BigDecimal("6.50"))
                    .imageUrl("https://images.unsplash.com/photo-1517701604599-bb29b565090c?auto=format&fit=crop&w=600&q=80")
                    .vegNonveg(MenuItem.FoodType.VEG)
                    .isAvailable(true)
                    .displayOrder(5)
                    .status(MenuItem.Status.ACTIVE)
                    .build());

            log.info("Seeding default menu items into MySQL database...");
        }

        // 6. Seed QR Codes
        List<QrCode> existingQrs = qrCodeRepository.findByRestaurantId(restaurant.getId());
        if (existingQrs.isEmpty()) {
            qrCodeRepository.save(QrCode.builder()
                    .restaurant(restaurant)
                    .branch(mainBranch)
                    .tableNumber("Table 01")
                    .label("Table 01")
                    .token("table-01-preview")
                    .status(QrCode.Status.ACTIVE)
                    .build());

            qrCodeRepository.save(QrCode.builder()
                    .restaurant(restaurant)
                    .branch(mainBranch)
                    .tableNumber("Table 02")
                    .label("Table 02")
                    .token("table-02-preview")
                    .status(QrCode.Status.ACTIVE)
                    .build());

            qrCodeRepository.save(QrCode.builder()
                    .restaurant(restaurant)
                    .branch(mainBranch)
                    .tableNumber("Table 05")
                    .label("Table 05")
                    .token("table-05-preview")
                    .status(QrCode.Status.ACTIVE)
                    .build());

            log.info("Seeding default QR codes into MySQL database...");
        }

        log.info("MySQL database data check & seeding completed successfully!");
    }
}
