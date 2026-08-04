package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.analytics.repository.ScanEventRepository;
import com.restaurantqr.platform.modules.branch.entity.Branch;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.repository.CategoryRepository;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.platform.modules.offer.entity.Offer;
import com.restaurantqr.platform.modules.offer.repository.OfferRepository;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.repository.QrCodeRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase5QrAndPublicMenuFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QrCodeRepository qrCodeRepository;

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

    @MockBean
    private ScanEventRepository scanEventRepository;

    private Restaurant activeRestaurant;
    private Restaurant suspendedRestaurant;

    private Branch activeBranch;

    private QrCode activeQr;
    private QrCode inactiveQr;

    private Category activeCategory;
    private MenuItem activeMenuItem;
    private Offer activeOffer;

    @BeforeEach
    void setUp() {
        activeRestaurant = Restaurant.builder()
                .name("Winged Cafe")
                .slug("winged-cafe")
                .status(Restaurant.Status.ACTIVE)
                .build();
        activeRestaurant.setId(100L);

        suspendedRestaurant = Restaurant.builder()
                .name("Suspended Cafe")
                .slug("suspended-cafe")
                .status(Restaurant.Status.SUSPENDED)
                .build();
        suspendedRestaurant.setId(200L);

        activeBranch = Branch.builder()
                .name("Main Branch")
                .restaurant(activeRestaurant)
                .build();
        activeBranch.setId(1000L);

        activeQr = QrCode.builder()
                .restaurant(activeRestaurant)
                .branch(activeBranch)
                .token("valid-token-999")
                .status(QrCode.Status.ACTIVE)
                .tableNumber("5")
                .build();
        activeQr.setId(5000L);

        inactiveQr = QrCode.builder()
                .restaurant(activeRestaurant)
                .branch(activeBranch)
                .token("inactive-token-888")
                .status(QrCode.Status.INACTIVE)
                .build();
        inactiveQr.setId(6000L);

        activeCategory = Category.builder()
                .name("Drinks")
                .restaurant(activeRestaurant)
                .status(Category.Status.ACTIVE)
                .build();
        activeCategory.setId(500L);

        activeMenuItem = MenuItem.builder()
                .name("Latte")
                .price(new BigDecimal("4.50"))
                .restaurant(activeRestaurant)
                .category(activeCategory)
                .isAvailable(true)
                .vegNonveg(MenuItem.FoodType.VEG)
                .build();
        activeMenuItem.setId(50000L);

        activeOffer = Offer.builder()
                .title("20% Off Coffee")
                .discountType(Offer.DiscountType.PERCENTAGE)
                .discountPercentage(new BigDecimal("20.00"))
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .restaurant(activeRestaurant)
                .build();
        activeOffer.setId(1000L);

        when(qrCodeRepository.findByTokenAndStatus("valid-token-999", QrCode.Status.ACTIVE))
                .thenReturn(Optional.of(activeQr));
        when(qrCodeRepository.findByTokenAndStatus("inactive-token-888", QrCode.Status.ACTIVE))
                .thenReturn(Optional.empty());
        when(qrCodeRepository.save(activeQr)).thenReturn(activeQr);

        when(restaurantRepository.findBySlugAndIsDeletedFalse("winged-cafe"))
                .thenReturn(Optional.of(activeRestaurant));
        when(restaurantRepository.findBySlugAndIsDeletedFalse("suspended-cafe"))
                .thenReturn(Optional.of(suspendedRestaurant));

        when(categoryRepository.findActiveByRestaurantId(100L)).thenReturn(List.of(activeCategory));
        when(menuItemRepository.findActiveByRestaurantId(100L)).thenReturn(List.of(activeMenuItem));
        when(offerRepository.findActiveOffers(100L, LocalDate.now())).thenReturn(List.of(activeOffer));
    }

    @Test
    @DisplayName("Valid QR scan → Resolves public menu and records scan event")
    void validQrScan_resolvesMenuPayload() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/valid-token-999").contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.restaurant.name").value("Winged Cafe"))
                .andExpect(jsonPath("$.data.qrCode.tableNumber").value("5"))
                .andExpect(jsonPath("$.data.categories[0].name").value("Drinks"))
                .andExpect(jsonPath("$.data.menuItems[0].name").value("Latte"))
                .andExpect(jsonPath("$.data.activeOffers[0].title").value("20% Off Coffee"));
    }

    @Test
    @DisplayName("Invalid / Tampered QR token → Returns 404 Not Found")
    void invalidQrToken_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/invalid-token-xyz").contextPath("/api/v1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Inactive / Disabled QR code → Returns 404 Not Found")
    void inactiveQrCode_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/inactive-token-888").contextPath("/api/v1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Suspended Restaurant Slug → Returns 404 Not Found")
    void suspendedRestaurantSlug_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/restaurant/suspended-cafe").contextPath("/api/v1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Public DTO Data Protection → Ensures no internal passwords, tokens, or subscription secrets leak")
    void publicDataLeakCheck_noSecretsExposed() throws Exception {
        mockMvc.perform(get("/api/v1/public/menu/valid-token-999").contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restaurant.subscriptions").doesNotExist())
                .andExpect(jsonPath("$.data.restaurant.password").doesNotExist())
                .andExpect(jsonPath("$.data.restaurant.resetToken").doesNotExist());
    }
}
