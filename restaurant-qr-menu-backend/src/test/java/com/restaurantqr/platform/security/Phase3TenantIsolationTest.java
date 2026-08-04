package com.restaurantqr.platform.security;

import com.restaurantqr.platform.RestaurantQrApplication;
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
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase3TenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

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
    private QrCodeRepository qrCodeRepository;

    private Restaurant restaurantA;
    private Restaurant restaurantB;

    private User ownerA;
    private User ownerB;

    private Branch branchA;
    private Branch branchB;

    private Category categoryA;
    private Category categoryB;

    private MenuItem menuItemA;
    private MenuItem menuItemB;

    private Offer offerA;
    private Offer offerB;

    private QrCode qrCodeA;
    private QrCode qrCodeB;

    private String tokenOwnerA;

    @BeforeEach
    void setUp() {
        restaurantA = Restaurant.builder().name("Restaurant A").slug("rest-a").build();
        restaurantA.setId(1L);

        restaurantB = Restaurant.builder().name("Restaurant B").slug("rest-b").build();
        restaurantB.setId(2L);

        ownerA = User.builder().email("ownerA@example.com").password("enc").role(User.Role.RESTAURANT_OWNER).status(User.Status.ACTIVE).restaurant(restaurantA).build();
        ownerA.setId(10L);

        ownerB = User.builder().email("ownerB@example.com").password("enc").role(User.Role.RESTAURANT_OWNER).status(User.Status.ACTIVE).restaurant(restaurantB).build();
        ownerB.setId(20L);

        branchA = Branch.builder().name("Branch A").restaurant(restaurantA).build();
        branchA.setId(100L);

        branchB = Branch.builder().name("Branch B").restaurant(restaurantB).build();
        branchB.setId(200L);

        categoryA = Category.builder().name("Category A").restaurant(restaurantA).displayOrder(0).build();
        categoryA.setId(1000L);

        categoryB = Category.builder().name("Category B").restaurant(restaurantB).displayOrder(0).build();
        categoryB.setId(2000L);

        menuItemA = MenuItem.builder().name("Item A").price(BigDecimal.TEN).restaurant(restaurantA).category(categoryA).vegNonveg(MenuItem.FoodType.VEG).build();
        menuItemA.setId(10000L);

        menuItemB = MenuItem.builder().name("Item B").price(BigDecimal.TEN).restaurant(restaurantB).category(categoryB).vegNonveg(MenuItem.FoodType.VEG).build();
        menuItemB.setId(20000L);

        offerA = Offer.builder().title("Offer A").restaurant(restaurantA).discountType(Offer.DiscountType.PERCENTAGE).discountPercentage(BigDecimal.TEN).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(7)).build();
        offerA.setId(100000L);

        offerB = Offer.builder().title("Offer B").restaurant(restaurantB).discountType(Offer.DiscountType.PERCENTAGE).discountPercentage(BigDecimal.TEN).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(7)).build();
        offerB.setId(200000L);

        qrCodeA = QrCode.builder().restaurant(restaurantA).branch(branchA).token("tokenA").status(QrCode.Status.ACTIVE).build();
        qrCodeA.setId(1000000L);

        qrCodeB = QrCode.builder().restaurant(restaurantB).branch(branchB).token("tokenB").status(QrCode.Status.ACTIVE).build();
        qrCodeB.setId(2000000L);

        tokenOwnerA = jwtTokenProvider.generateAccessToken(new JwtUserDetails(ownerA));

        when(userRepository.findByEmailAndIsDeletedFalse("ownerA@example.com")).thenReturn(Optional.of(ownerA));
        when(userRepository.findByEmailAndIsDeletedFalse("ownerB@example.com")).thenReturn(Optional.of(ownerB));

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurantA));
        when(restaurantRepository.findById(2L)).thenReturn(Optional.of(restaurantB));

        when(branchRepository.findById(100L)).thenReturn(Optional.of(branchA));
        when(branchRepository.findById(200L)).thenReturn(Optional.of(branchB));

        when(categoryRepository.findById(1000L)).thenReturn(Optional.of(categoryA));
        when(categoryRepository.findById(2000L)).thenReturn(Optional.of(categoryB));

        when(menuItemRepository.findById(10000L)).thenReturn(Optional.of(menuItemA));
        when(menuItemRepository.findById(20000L)).thenReturn(Optional.of(menuItemB));

        when(offerRepository.findById(100000L)).thenReturn(Optional.of(offerA));
        when(offerRepository.findById(200000L)).thenReturn(Optional.of(offerB));

        when(qrCodeRepository.findById(1000000L)).thenReturn(Optional.of(qrCodeA));
        when(qrCodeRepository.findById(2000000L)).thenReturn(Optional.of(qrCodeB));
    }

    // ─── RESTAURANT IDOR ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Owner A GET /restaurants/2 → 403 Forbidden")
    void ownerA_getRestaurantB_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/2")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner A PUT /restaurants/2 → 403 Forbidden")
    void ownerA_updateRestaurantB_forbidden() throws Exception {
        String body = """
                {
                    "name": "Hacked Rest B",
                    "slug": "hacked-b"
                }
                """;
        mockMvc.perform(put("/api/v1/restaurants/2")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }

    // ─── BRANCH IDOR ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Owner A GET /restaurants/2/branches/200 → 403 Forbidden")
    void ownerA_getBranchB_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/2/branches/200")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner A GET /restaurants/1/branches/200 (IDOR under own path) → 403 Forbidden")
    void ownerA_getBranchB_underOwnPath_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/1/branches/200")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }

    // ─── CATEGORY IDOR ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Owner A GET /restaurants/1/categories/2000 → 403 Forbidden")
    void ownerA_getCategoryB_underOwnPath_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/1/categories/2000")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Owner A DELETE /restaurants/2/categories/2000 → 403 Forbidden")
    void ownerA_deleteCategoryB_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/restaurants/2/categories/2000")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }

    // ─── MENU ITEM IDOR & CROSS-TENANT RELATIONSHIP INJECTION ────────────────

    @Test
    @DisplayName("Owner A GET /restaurants/1/menu-items/category/2000 (Category B ID) → 404 Not Found")
    void ownerA_getMenuItemsOfCategoryB_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/1/menu-items/category/2000")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Owner A POST /restaurants/1/menu-items with categoryId=2000 (Category B) → 404 (Category not found for Rest A)")
    void ownerA_createMenuItem_crossTenantCategory_rejected() throws Exception {
        String body = """
                {
                    "name": "Malicious Item",
                    "price": 15.00,
                    "categoryId": 2000,
                    "vegNonveg": "VEG"
                }
                """;
        mockMvc.perform(post("/api/v1/restaurants/1/menu-items")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isNotFound());
    }

    // ─── OFFER IDOR ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Owner A DELETE /restaurants/1/offers/200000 (Offer B) → 403 Forbidden")
    void ownerA_deleteOfferB_underOwnPath_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/restaurants/1/offers/200000")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }

    // ─── QR CODE IDOR & CROSS-TENANT RELATIONSHIP INJECTION ───────────────────

    @Test
    @DisplayName("Owner A POST /restaurants/1/qr-codes with branchId=200 (Branch B) → 404 (Branch B not found for Rest A)")
    void ownerA_generateQrCode_crossTenantBranch_rejected() throws Exception {
        String body = """
                {
                    "branchId": 200,
                    "tableNumber": "12",
                    "label": "Table 12"
                }
                """;
        mockMvc.perform(post("/api/v1/restaurants/1/qr-codes")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isNotFound());
    }

    // ─── USER IDOR ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Owner A GET /restaurants/1/users/20 (Owner B user ID) → 404 Not Found")
    void ownerA_getUserB_underOwnPath_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/1/users/20")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isNotFound());
    }

    // ─── ANALYTICS IDOR ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Owner A GET /analytics/restaurants/2/dashboard → 403 Forbidden")
    void ownerA_getAnalyticsB_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/restaurants/2/dashboard")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }
}
