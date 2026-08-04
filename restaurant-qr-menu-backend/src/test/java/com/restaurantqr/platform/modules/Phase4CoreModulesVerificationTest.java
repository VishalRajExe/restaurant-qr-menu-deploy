package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.branch.service.BranchRequest;
import com.restaurantqr.platform.modules.branch.service.BranchService;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.repository.CategoryRepository;
import com.restaurantqr.platform.modules.category.service.CategoryRequest;
import com.restaurantqr.platform.modules.category.service.CategoryService;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemRequest;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemService;
import com.restaurantqr.platform.modules.offer.entity.Offer;
import com.restaurantqr.platform.modules.offer.repository.OfferRepository;
import com.restaurantqr.platform.modules.offer.service.OfferRequest;
import com.restaurantqr.platform.modules.offer.service.OfferService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantRequest;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
class Phase4CoreModulesVerificationTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private BranchService branchService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private OfferService offerService;

    @MockBean
    private RestaurantRepository restaurantRepository;

    @MockBean
    private BranchRepository branchRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private MenuItemRepository menuItemRepository;

    @MockBean
    private OfferRepository offerRepository;

    private Restaurant restaurant;
    private User owner;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder()
                .name("Sample Rest")
                .slug("sample-rest")
                .subscriptionPlan(Restaurant.SubscriptionPlan.PROFESSIONAL)
                .build();
        restaurant.setId(10L);

        owner = User.builder()
                .email("owner@sample.com")
                .password("enc")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(restaurant)
                .build();
        owner.setId(100L);

        var userDetails = new JwtUserDetails(owner);
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(restaurant));
    }

    @Test
    @DisplayName("Restaurant Module: Conflict handling on duplicate slug")
    void restaurant_duplicateSlug_throwsConflict() {
        when(restaurantRepository.existsBySlugAndIsDeletedFalse("existing-slug")).thenReturn(true);
        var req = new RestaurantRequest();
        req.name = "Existing";
        req.slug = "existing-slug";

        assertThrows(com.restaurantqr.platform.common.ConflictException.class, () -> restaurantService.create(req));
    }

    @Test
    @DisplayName("Branch Module: CRUD & soft delete")
    void branch_crudAndSoftDelete() {
        var req = new BranchRequest();
        req.name = "Downtown";
        req.address = "123 Main St";

        var branch = Branch.builder().name("Downtown").restaurant(restaurant).build();
        branch.setId(50L);
        when(branchRepository.save(any(Branch.class))).thenReturn(branch);
        when(branchRepository.findById(50L)).thenReturn(Optional.of(branch));

        var created = branchService.create(10L, req);
        assertNotNull(created);
        assertEquals("Downtown", created.getName());

        branchService.delete(50L, 10L);
        assertTrue(branch.getIsDeleted());
    }

    @Test
    @DisplayName("Category Module: Reorder & soft delete")
    void category_crudAndSoftDelete() {
        var category = Category.builder().name("Starters").restaurant(restaurant).displayOrder(0).build();
        category.setId(500L);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);
        when(categoryRepository.findById(500L)).thenReturn(Optional.of(category));

        categoryService.toggleStatus(500L, 10L);
        assertEquals(Category.Status.INACTIVE, category.getStatus());

        categoryService.delete(500L, 10L);
        assertTrue(category.getIsDeleted());
    }

    @Test
    @DisplayName("MenuItem Module: Price validation & Availability toggle")
    void menuItem_priceAndAvailability() {
        var category = Category.builder().name("Starters").restaurant(restaurant).build();
        category.setId(500L);
        when(categoryRepository.findById(500L)).thenReturn(Optional.of(category));

        var req = new MenuItemRequest();
        req.categoryId = 500L;
        req.name = "Spring Roll";
        req.price = new BigDecimal("8.50");
        req.vegNonveg = MenuItem.FoodType.VEG;

        var item = MenuItem.builder()
                .name("Spring Roll")
                .price(new BigDecimal("8.50"))
                .restaurant(restaurant)
                .category(category)
                .isAvailable(true)
                .build();
        item.setId(5000L);

        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(item);
        when(menuItemRepository.findById(5000L)).thenReturn(Optional.of(item));

        var created = menuItemService.create(10L, req);
        assertEquals(new BigDecimal("8.50"), created.getPrice());

        menuItemService.updateAvailability(5000L, 10L, false);
        assertFalse(item.getIsAvailable());
    }

    @Test
    @DisplayName("Offer Module: Date range validation & discount percentage range validation")
    void offer_validation_dateRangeAndPercentage() {
        var invalidDates = new OfferRequest();
        invalidDates.title = "Summer Sale";
        invalidDates.discountType = Offer.DiscountType.PERCENTAGE;
        invalidDates.discountPercentage = new BigDecimal("15");
        invalidDates.startDate = LocalDate.now().plusDays(5);
        invalidDates.endDate = LocalDate.now(); // End before start!

        assertThrows(com.restaurantqr.platform.common.BadRequestException.class,
                () -> offerService.create(10L, invalidDates));

        var invalidPercentage = new OfferRequest();
        invalidPercentage.title = "Invalid Discount";
        invalidPercentage.discountType = Offer.DiscountType.PERCENTAGE;
        invalidPercentage.discountPercentage = new BigDecimal("150"); // > 100%
        invalidPercentage.startDate = LocalDate.now();
        invalidPercentage.endDate = LocalDate.now().plusDays(5);

        assertThrows(com.restaurantqr.platform.common.BadRequestException.class,
                () -> offerService.create(10L, invalidPercentage));
    }
}
