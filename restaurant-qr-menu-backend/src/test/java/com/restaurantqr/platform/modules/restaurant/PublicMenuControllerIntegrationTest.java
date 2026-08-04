package com.restaurantqr.platform.modules.restaurant;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.service.CategoryService;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.service.MenuItemService;
import com.restaurantqr.platform.modules.offer.entity.Offer;
import com.restaurantqr.platform.modules.offer.service.OfferService;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.service.QrCodeService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.analytics.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicMenuControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrCodeService qrCodeService;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private MenuItemService menuItemService;

    @MockBean
    private OfferService offerService;

    @MockBean
    private AnalyticsService analyticsService;

    private Restaurant testRestaurant;
    private QrCode testQrCode;

    @BeforeEach
    void setUp() {
        testRestaurant = new Restaurant();
        testRestaurant.setId(1L);
        testRestaurant.setSlug("test-restaurant");
        testRestaurant.setName("Test Restaurant");

        testQrCode = new QrCode();
        testQrCode.setId(1L);
        testQrCode.setToken("valid-token-123");
        testQrCode.setRestaurant(testRestaurant);

        // Mock service responses
        when(qrCodeService.scan(anyString())).thenReturn(null);
        when(qrCodeService.scan("valid-token-123")).thenReturn(testQrCode);
        when(restaurantService.findBySlug(anyString())).thenReturn(null);
        when(restaurantService.findBySlug("test-restaurant")).thenReturn(testRestaurant);
        when(restaurantService.findById(1L)).thenReturn(testRestaurant);
        when(categoryService.findActiveByRestaurant(1L)).thenReturn(Collections.emptyList());
        when(menuItemService.getPublicMenu(1L)).thenReturn(Collections.emptyList());
        when(offerService.getActiveOffers(1L)).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("GET /api/v1/public/menu/valid-token returns 200")
    void getMenuByValidToken_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/valid-token-123").contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/v1/public/menu/invalid-token returns 404")
    void getMenuByInvalidToken_returns404() throws Exception {
        when(qrCodeService.scan("invalid-token-xyz")).thenThrow(new com.restaurantqr.platform.common.ResourceNotFoundException("QR code not found or inactive"));
        mockMvc.perform(get("/api/v1/public/menu/invalid-token-xyz").contextPath("/api/v1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/public/menu/restaurant/valid-slug returns 200")
    void getMenuByValidSlug_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/restaurant/test-restaurant").contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/v1/public/menu/restaurant/invalid-slug returns 404")
    void getMenuByInvalidSlug_returns404() throws Exception {
        when(restaurantService.findBySlug("nonexistent-slug-xyz")).thenThrow(new com.restaurantqr.platform.common.ResourceNotFoundException("Restaurant not found: nonexistent-slug-xyz"));
        mockMvc.perform(get("/api/v1/public/menu/restaurant/nonexistent-slug-xyz").contextPath("/api/v1"))
                .andExpect(status().isNotFound());
    }
}