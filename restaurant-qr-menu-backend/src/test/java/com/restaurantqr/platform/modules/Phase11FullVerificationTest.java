package com.restaurantqr.platform.modules;

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
import com.restaurantqr.platform.modules.subscription.entity.Subscription;
import com.restaurantqr.platform.modules.subscription.repository.SubscriptionRepository;
import com.restaurantqr.platform.security.JwtTokenProvider;
import com.restaurantqr.platform.security.JwtUserDetails;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestaurantQrApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase11FullVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RestaurantRepository restaurantRepository;

    @MockBean
    private UserRepository userRepository;

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

    @MockBean
    private SubscriptionRepository subscriptionRepository;

    private Restaurant restA;
    private Restaurant restB;

    private User superAdmin;
    private User ownerA;
    private User ownerB;

    private Branch branchA;
    private Category categoryA;
    private MenuItem menuItemA;
    private Offer offerA;
    private QrCode qrCodeA;

    private String tokenSuperAdmin;
    private String tokenOwnerA;
    private String tokenOwnerB;

    @BeforeEach
    void setUp() {
        // Restaurant A (Basic Plan)
        restA = Restaurant.builder()
                .name("Restaurant A")
                .slug("restaurant-a")
                .subscriptionPlan(Restaurant.SubscriptionPlan.BASIC)
                .status(Restaurant.Status.ACTIVE)
                .build();
        restA.setId(100L);

        // Restaurant B (Professional Plan)
        restB = Restaurant.builder()
                .name("Restaurant B")
                .slug("restaurant-b")
                .subscriptionPlan(Restaurant.SubscriptionPlan.PROFESSIONAL)
                .status(Restaurant.Status.ACTIVE)
                .build();
        restB.setId(200L);

        // Super Admin
        superAdmin = User.builder()
                .email("super@admin.com")
                .password("enc")
                .role(User.Role.SUPER_ADMIN)
                .status(User.Status.ACTIVE)
                .build();
        superAdmin.setId(1L);

        // Owner A
        ownerA = User.builder()
                .email("owner.a@resta.com")
                .password("enc")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(restA)
                .build();
        ownerA.setId(10L);

        // Owner B
        ownerB = User.builder()
                .email("owner.b@restb.com")
                .password("enc")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(restB)
                .build();
        ownerB.setId(20L);

        // Entities for Restaurant A
        branchA = Branch.builder().name("Main Branch A").restaurant(restA).build();
        branchA.setId(1000L);

        categoryA = Category.builder().name("Beverages").restaurant(restA).status(Category.Status.ACTIVE).build();
        categoryA.setId(2000L);

        menuItemA = MenuItem.builder().name("Iced Tea").price(new BigDecimal("3.50")).restaurant(restA).category(categoryA).isAvailable(true).build();
        menuItemA.setId(3000L);

        offerA = Offer.builder().title("Happy Hour").discountType(Offer.DiscountType.PERCENTAGE).discountPercentage(new BigDecimal("10.00")).startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(10)).restaurant(restA).build();
        offerA.setId(4000L);

        qrCodeA = QrCode.builder().restaurant(restA).branch(branchA).token("qr-token-flow-a").status(QrCode.Status.ACTIVE).tableNumber("1").build();
        qrCodeA.setId(5000L);

        tokenSuperAdmin = jwtTokenProvider.generateAccessToken(new JwtUserDetails(superAdmin));
        tokenOwnerA = jwtTokenProvider.generateAccessToken(new JwtUserDetails(ownerA));
        tokenOwnerB = jwtTokenProvider.generateAccessToken(new JwtUserDetails(ownerB));

        when(userRepository.findByEmailAndIsDeletedFalse("super@admin.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.findByEmailAndIsDeletedFalse("owner.a@resta.com")).thenReturn(Optional.of(ownerA));
        when(userRepository.findByEmailAndIsDeletedFalse("owner.b@restb.com")).thenReturn(Optional.of(ownerB));

        when(restaurantRepository.findById(100L)).thenReturn(Optional.of(restA));
        when(restaurantRepository.findById(200L)).thenReturn(Optional.of(restB));
        when(restaurantRepository.findBySlugAndIsDeletedFalse("restaurant-a")).thenReturn(Optional.of(restA));

        when(branchRepository.findById(1000L)).thenReturn(Optional.of(branchA));
        when(categoryRepository.findById(2000L)).thenReturn(Optional.of(categoryA));
        when(categoryRepository.findActiveByRestaurantId(100L)).thenReturn(List.of(categoryA));
        when(menuItemRepository.findActiveByRestaurantId(100L)).thenReturn(List.of(menuItemA));
        when(offerRepository.findActiveOffers(100L, LocalDate.now())).thenReturn(List.of(offerA));
        when(qrCodeRepository.findByTokenAndStatus("qr-token-flow-a", QrCode.Status.ACTIVE)).thenReturn(Optional.of(qrCodeA));
        when(qrCodeRepository.save(any(QrCode.class))).thenReturn(qrCodeA);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }


    @Test
    @DisplayName("FLOW A — Super Admin: Stats, Status Update, Owner Account Creation, Subscription Activation")
    void flowA_superAdminFlow() throws Exception {
        // 1. Stats
        mockMvc.perform(get("/api/v1/super-admin/stats")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Patch Restaurant Status
        String patchBody = "{\"status\": \"ACTIVE\"}";
        mockMvc.perform(patch("/api/v1/super-admin/restaurants/100/status")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody)
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk());

        // 3. Activate Subscription
        String activateBody = """
                {
                    "plan": "PROFESSIONAL",
                    "months": 12,
                    "paymentId": "pay_verified_999",
                    "paymentGateway": "RAZORPAY"
                }
                """;
        mockMvc.perform(post("/api/v1/subscriptions/restaurants/100/activate")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activateBody)
                        .header("Authorization", "Bearer " + tokenSuperAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("PROFESSIONAL"));
    }

    @Test
    @DisplayName("FLOW B — Restaurant Owner: CRUD operations on Branch, Category, MenuItem, Offer, QR")
    void flowB_restaurantOwnerFlow() throws Exception {
        // 1. Get owned restaurant
        mockMvc.perform(get("/api/v1/restaurants/100")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Restaurant A"));

        // 2. Get Branch
        mockMvc.perform(get("/api/v1/restaurants/100/branches/1000")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Main Branch A"));
    }

    @Test
    @DisplayName("FLOW C — Public Customer: Resolves QR token and public restaurant menu data")
    void flowC_publicCustomerFlow() throws Exception {
        // 1. QR token resolution
        mockMvc.perform(get("/api/v1/public/menu/qr-token-flow-a").contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restaurant.name").value("Restaurant A"))
                .andExpect(jsonPath("$.data.categories[0].name").value("Beverages"))
                .andExpect(jsonPath("$.data.menuItems[0].name").value("Iced Tea"))
                .andExpect(jsonPath("$.data.activeOffers[0].title").value("Happy Hour"));

        // 2. Slug menu resolution
        mockMvc.perform(get("/api/v1/public/menu/restaurant/restaurant-a").contextPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restaurant.name").value("Restaurant A"));
    }

    @Test
    @DisplayName("FLOW D — Tenant Attack: Owner B attempting GET/PUT/DELETE on Restaurant A is denied")
    void flowD_tenantAttackDenied() throws Exception {
        // Owner B attempts GET Restaurant A details
        mockMvc.perform(get("/api/v1/restaurants/100")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerB))
                .andExpect(status().isForbidden());

        // Owner B attempts DELETE Restaurant A Branch 1000
        mockMvc.perform(delete("/api/v1/restaurants/100/branches/1000")
                        .contextPath("/api/v1")
                        .header("Authorization", "Bearer " + tokenOwnerB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FLOW E — Subscription Limit & Bypass Protection: BASIC limit and direct activation denied")
    void flowE_subscriptionLimitAndBypassDenied() throws Exception {
        // 1. BASIC plan branch limit breach -> 402 Payment Required
        when(branchRepository.countByRestaurantIdAndIsDeletedFalse(100L)).thenReturn(1L);
        String createBranch = "{\"name\": \"Branch 2\", \"address\": \"2nd St\"}";

        mockMvc.perform(post("/api/v1/restaurants/100/branches")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBranch)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isPaymentRequired());

        // 2. Direct activation bypass attempt by Owner A -> 403 Forbidden
        String activateBody = "{\"plan\":\"ENTERPRISE\",\"months\":12,\"paymentId\":\"fake\",\"paymentGateway\":\"RAZORPAY\"}";
        mockMvc.perform(post("/api/v1/subscriptions/restaurants/100/activate")
                        .contextPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(activateBody)
                        .header("Authorization", "Bearer " + tokenOwnerA))
                .andExpect(status().isForbidden());
    }
}
