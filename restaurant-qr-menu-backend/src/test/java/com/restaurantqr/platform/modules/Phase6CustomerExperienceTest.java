package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.service.CategoryRequest;
import com.restaurantqr.platform.modules.category.service.CategoryService;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemRequest;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantRequest;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@Transactional
class Phase6CustomerExperienceTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MenuItemService menuItemService;

    private Restaurant testRestaurant;
    private Category mainCategory;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        RestaurantRequest req = new RestaurantRequest();
        req.name = "Customer Experience Gourmet";
        req.slug = "cust-exp-" + System.currentTimeMillis();
        testRestaurant = restaurantService.create(req);

        ownerUser = userRepository.save(User.builder()
                .name("Customer Exp Owner")
                .email("custowner-" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(testRestaurant)
                .build());

        JwtUserDetails details = new JwtUserDetails(ownerUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));

        CategoryRequest catReq = new CategoryRequest();
        catReq.name = "Signature Dishes";
        mainCategory = categoryService.create(testRestaurant.getId(), catReq);
    }

    @Test
    @DisplayName("1. Badges & Macros: Popular, Chef Special, Spice Level, Protein, Fat, Carbs, Allergens & Dietary Tags")
    void testMenuItemBadgesAndMacros() {
        MenuItemRequest itemReq = new MenuItemRequest();
        itemReq.categoryId = mainCategory.getId();
        itemReq.name = "Fiery Vegan Paneer Tikka";
        itemReq.description = "Smoky grilled cottage cheese with aromatic spices";
        itemReq.price = new BigDecimal("349.00");
        itemReq.vegNonveg = MenuItem.FoodType.VEG;
        itemReq.isPopular = true;
        itemReq.isChefSpecial = true;
        itemReq.spiceLevel = 4;
        itemReq.calories = 380;
        itemReq.proteinGrams = new BigDecimal("22.50");
        itemReq.fatGrams = new BigDecimal("14.00");
        itemReq.carbsGrams = new BigDecimal("18.00");
        itemReq.allergens = "Dairy, Mustard";
        itemReq.isVegan = false;
        itemReq.isHalal = true;
        itemReq.isGlutenFree = true;
        itemReq.mealType = MenuItem.MealType.DINNER;

        MenuItem item = menuItemService.create(testRestaurant.getId(), itemReq);

        assertNotNull(item.getId());
        assertTrue(item.getIsPopular());
        assertTrue(item.getIsChefSpecial());
        assertEquals(4, item.getSpiceLevel());
        assertEquals(380, item.getCalories());
        assertEquals(new BigDecimal("22.50"), item.getProteinGrams());
        assertEquals("Dairy, Mustard", item.getAllergens());
        assertTrue(item.getIsHalal());
        assertTrue(item.getIsGlutenFree());
        assertEquals(MenuItem.MealType.DINNER, item.getMealType());
    }

    @Test
    @DisplayName("2. Customer Favorites Engine: Toggle and list customer favorite items by device token")
    void testCustomerFavoritesEngine() {
        MenuItemRequest itemReq = new MenuItemRequest();
        itemReq.categoryId = mainCategory.getId();
        itemReq.name = "Butter Chicken Bowl";
        itemReq.price = new BigDecimal("399.00");
        itemReq.vegNonveg = MenuItem.FoodType.NON_VEG;
        MenuItem item = menuItemService.create(testRestaurant.getId(), itemReq);

        String deviceToken = "device-ios-token-998877";

        boolean isFav = menuItemService.toggleFavorite(deviceToken, testRestaurant.getId(), item.getId());
        assertTrue(isFav);

        List<MenuItem> favorites = menuItemService.getFavorites(deviceToken, testRestaurant.getId());
        assertEquals(1, favorites.size());
        assertEquals(item.getId(), favorites.get(0).getId());

        boolean toggledOff = menuItemService.toggleFavorite(deviceToken, testRestaurant.getId(), item.getId());
        assertFalse(toggledOff);

        List<MenuItem> updatedFavs = menuItemService.getFavorites(deviceToken, testRestaurant.getId());
        assertTrue(updatedFavs.isEmpty());
    }

    @Test
    @DisplayName("3. Public Discovery Endpoints: Recommended, Recently Added, Combos, and Related Items")
    void testDiscoveryEndpoints() {
        MenuItemRequest comboReq = new MenuItemRequest();
        comboReq.categoryId = mainCategory.getId();
        comboReq.name = "Super Meal Combo";
        comboReq.price = new BigDecimal("499.00");
        comboReq.vegNonveg = MenuItem.FoodType.NON_VEG;
        comboReq.isCombo = true;
        comboReq.comboDescription = "Includes Burger + Fries + Soft Drink";
        comboReq.isPopular = true;
        MenuItem combo = menuItemService.create(testRestaurant.getId(), comboReq);

        MenuItemRequest sideReq = new MenuItemRequest();
        sideReq.categoryId = mainCategory.getId();
        sideReq.name = "Crispy Garlic Fries";
        sideReq.price = new BigDecimal("149.00");
        sideReq.vegNonveg = MenuItem.FoodType.VEG;
        MenuItem side = menuItemService.create(testRestaurant.getId(), sideReq);

        List<MenuItem> recommended = menuItemService.getRecommended(testRestaurant.getId());
        assertFalse(recommended.isEmpty());

        List<MenuItem> recentlyAdded = menuItemService.getRecentlyAdded(testRestaurant.getId());
        assertEquals(2, recentlyAdded.size());

        List<MenuItem> combos = menuItemService.getCombos(testRestaurant.getId());
        assertEquals(1, combos.size());
        assertEquals(combo.getId(), combos.get(0).getId());

        List<MenuItem> related = menuItemService.getRelatedItems(testRestaurant.getId(), combo.getId());
        assertEquals(1, related.size());
        assertEquals(side.getId(), related.get(0).getId());
    }
}
