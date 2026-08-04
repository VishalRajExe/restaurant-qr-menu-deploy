package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.audit.entity.AuditLog;
import com.restaurantqr.platform.audit.service.AuditLogService;
import com.restaurantqr.platform.config.RateLimitingFilter;
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
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.Permission;
import com.restaurantqr.platform.users.entity.StaffInvitation;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.StaffInvitationRepository;
import com.restaurantqr.platform.users.repository.UserRepository;
import com.restaurantqr.platform.users.service.StaffInvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@Transactional
class Phase2SaaSFoundationTest {

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private MenuItemService menuItemService;

    @Autowired
    private StaffInvitationService staffInvitationService;

    @Autowired
    private StaffInvitationRepository staffInvitationRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private Restaurant testRestaurant;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        testRestaurant = restaurantRepository.save(Restaurant.builder()
                .name("SaaS Foundation Test Bistro")
                .slug("saas-test-bistro-" + System.currentTimeMillis())
                .status(Restaurant.Status.ACTIVE)
                .subscriptionPlan(Restaurant.SubscriptionPlan.PROFESSIONAL)
                .build());


        ownerUser = userRepository.save(User.builder()
                .name("Test Owner")
                .email("owner-" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(testRestaurant)
                .build());

        JwtUserDetails details = new JwtUserDetails(ownerUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    @DisplayName("1. Granular Permission Matrix: User.Role permissions mapped into JwtUserDetails authorities")
    void testGranularPermissionAuthorities() {
        JwtUserDetails details = new JwtUserDetails(ownerUser);
        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();

        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_OWNER")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("MENU_CREATE")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("STAFF_MANAGE")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ANALYTICS_VIEW")));

        User manager = User.builder()
                .name("Manager")
                .email("manager@test.com")
                .password("pwd")
                .role(User.Role.MANAGER)
                .status(User.Status.ACTIVE)
                .build();
        JwtUserDetails managerDetails = new JwtUserDetails(manager);
        Collection<? extends GrantedAuthority> managerAuths = managerDetails.getAuthorities();

        assertTrue(managerAuths.stream().anyMatch(a -> a.getAuthority().equals("MENU_CREATE")));
        assertFalse(managerAuths.stream().anyMatch(a -> a.getAuthority().equals("STAFF_MANAGE")));
    }

    @Test
    @DisplayName("2. Staff Invitation Flow: Issue invite -> Accept invitation & set password")
    void testStaffInvitationFlow() {
        StaffInvitationService.InviteRequest inviteReq = new StaffInvitationService.InviteRequest();
        inviteReq.setEmail("invited.staff@test.com");
        inviteReq.setName("Invited Staff");
        inviteReq.setRole(User.Role.STAFF);

        StaffInvitationService.InvitationResponse response = staffInvitationService.createInvitation(testRestaurant.getId(), inviteReq);

        assertNotNull(response.getToken());
        assertEquals("invited.staff@test.com", response.getEmail());
        assertEquals(StaffInvitation.Status.PENDING, response.getStatus());

        StaffInvitationService.AcceptInvitationRequest acceptReq = new StaffInvitationService.AcceptInvitationRequest();
        acceptReq.setToken(response.getToken());
        acceptReq.setName("Invited Staff Member");
        acceptReq.setPassword("newSecurePassword123!");

        User createdUser = staffInvitationService.acceptInvitation(acceptReq);

        assertNotNull(createdUser.getId());
        assertEquals("invited.staff@test.com", createdUser.getEmail());
        assertEquals(User.Role.STAFF, createdUser.getRole());
        assertEquals(testRestaurant.getId(), createdUser.getRestaurant().getId());

        StaffInvitation updatedInvitation = staffInvitationRepository.findByToken(response.getToken()).orElseThrow();
        assertEquals(StaffInvitation.Status.ACCEPTED, updatedInvitation.getStatus());
    }

    @Test
    @DisplayName("3. Audit Log Recording & Activity Timeline: Log item creation and price changes")
    void testAuditLogRecordingAndActivityTimeline() {
        CategoryRequest catReq = new CategoryRequest();
        catReq.name = "Mains";
        Category category = categoryService.create(testRestaurant.getId(), catReq);

        MenuItemRequest itemReq = new MenuItemRequest();
        itemReq.name = "Special Burger";
        itemReq.price = new BigDecimal("299.00");
        itemReq.categoryId = category.getId();
        MenuItem item = menuItemService.create(testRestaurant.getId(), itemReq);

        itemReq.price = new BigDecimal("349.00");
        menuItemService.update(item.getId(), testRestaurant.getId(), itemReq);

        List<AuditLog> timeline = auditLogService.getActivityTimeline(testRestaurant.getId());
        assertFalse(timeline.isEmpty());

        AuditLog priceChangeLog = timeline.stream()
                .filter(l -> "ITEM_PRICE_CHANGED".equals(l.getAction()))
                .findFirst()
                .orElse(null);

        assertNotNull(priceChangeLog);
        assertEquals("₹299.00", priceChangeLog.getOldValue());
        assertEquals("₹349.00", priceChangeLog.getNewValue());
        assertEquals("MenuItem", priceChangeLog.getEntityType());
    }

    @Test
    @DisplayName("4. Soft Delete & Restore: Soft delete branch/category and verify restoration")
    void testSoftDeleteAndRestore() {
        BranchRequest branchReq = new BranchRequest();
        branchReq.name = "Downtown Branch";
        Branch branch = branchService.create(testRestaurant.getId(), branchReq);

        branchService.delete(branch.getId(), testRestaurant.getId());
        Branch deletedBranch = branchRepository.findById(branch.getId()).orElseThrow();
        assertTrue(deletedBranch.getIsDeleted());
        assertNotNull(deletedBranch.getDeletedAt());

        Branch restoredBranch = branchService.restore(branch.getId(), testRestaurant.getId());
        assertFalse(restoredBranch.getIsDeleted());
        assertNull(restoredBranch.getDeletedAt());
    }

    @Test
    @DisplayName("5. API Rate Limiting: Requests exceeding threshold return HTTP 429")
    void testRateLimitingFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/test-rate-limit");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        for (int i = 0; i < 100; i++) {
            MockHttpServletResponse tempResp = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(request, tempResp, new MockFilterChain());
            assertEquals(200, tempResp.getStatus());
        }

        rateLimitingFilter.doFilter(request, response, new MockFilterChain());
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("Rate limit exceeded"));
    }
}
