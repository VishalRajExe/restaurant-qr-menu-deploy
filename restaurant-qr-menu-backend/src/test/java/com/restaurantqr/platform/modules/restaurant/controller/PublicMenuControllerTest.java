package com.restaurantqr.platform.modules.restaurant.controller;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.service.CategoryService;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemService;
import com.restaurantqr.platform.modules.offer.entity.Offer;
import com.restaurantqr.platform.modules.offer.service.OfferService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PublicMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private MenuItemService menuItemService;

    @MockBean
    private OfferService offerService;

    private Restaurant activeRestaurant;
    private Category starterCategory;
    private MenuItem springRoll;

    @BeforeEach
    void setUp() {
        activeRestaurant = Restaurant.builder()
                .name("Winged Diner")
                .slug("winged-diner")
                .status(Restaurant.Status.ACTIVE)
                .build();
        activeRestaurant.setId(88L);

        starterCategory = Category.builder()
                .name("Starters")
                .restaurant(activeRestaurant)
                .status(Category.Status.ACTIVE)
                .build();
        starterCategory.setId(880L);

        springRoll = MenuItem.builder()
                .name("Veg Spring Roll")
                .price(new BigDecimal("6.99"))
                .restaurant(activeRestaurant)
                .category(starterCategory)
                .isAvailable(true)
                .vegNonveg(MenuItem.FoodType.VEG)
                .build();
        springRoll.setId(8800L);

        when(restaurantService.findBySlug("winged-diner")).thenReturn(activeRestaurant);
        when(categoryService.findActiveByRestaurant(88L)).thenReturn(List.of(starterCategory));
        when(menuItemService.getPublicMenu(88L)).thenReturn(List.of(springRoll));
        when(offerService.getActiveOffers(88L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("Public Menu by Slug: Returns active menu items and categories")
    void getMenuBySlug_returnsPublicMenu() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/restaurant/winged-diner").contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.restaurant.name").value("Winged Diner"))
                .andExpect(jsonPath("$.data.categories[0].name").value("Starters"))
                .andExpect(jsonPath("$.data.menuItems[0].name").value("Veg Spring Roll"));
    }
}