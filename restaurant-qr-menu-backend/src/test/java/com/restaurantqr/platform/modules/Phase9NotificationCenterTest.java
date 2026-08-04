package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.notification.entity.Notification;
import com.restaurantqr.platform.modules.notification.service.NotificationDispatcherService;
import com.restaurantqr.platform.modules.notification.service.NotificationService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@Transactional
class Phase9NotificationCenterTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationDispatcherService notificationDispatcherService;

    @Autowired
    private NotificationService notificationService;

    private Restaurant testRestaurant;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        RestaurantRequest req = new RestaurantRequest();
        req.name = "Notification Hub Bistro";
        req.slug = "notif-bistro-" + System.currentTimeMillis();
        testRestaurant = restaurantService.create(req);

        ownerUser = userRepository.save(User.builder()
                .name("Notif Owner")
                .email("notifowner-" + System.currentTimeMillis() + "@test.com")
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
    @DisplayName("1. Multi-Channel Dispatch: Dispatch PAYMENT_RECEIVED, SUBSCRIPTION_EXPIRING, OFFER_ENDING, NEW_STAFF_JOINED & QR_GENERATED")
    void testMultiChannelDispatch() {
        notificationDispatcherService.dispatchPaymentReceived(ownerUser, testRestaurant, "1499.00", "INV-99901");
        notificationDispatcherService.dispatchSubscriptionExpiring(ownerUser, testRestaurant, 3);
        notificationDispatcherService.dispatchOfferEnding(ownerUser, testRestaurant, "Weekend 20% Off");
        notificationDispatcherService.dispatchNewStaffJoined(ownerUser, testRestaurant, "Chef Alex");
        notificationDispatcherService.dispatchQrGenerated(ownerUser, testRestaurant, "Main Branch Table #5");

        Page<Notification> page = notificationService.getUserNotifications(ownerUser.getId(), PageRequest.of(0, 10));
        assertEquals(5, page.getTotalElements());
    }

    @Test
    @DisplayName("2. Inbox Unread Counter: Verify accurate unread notification counts")
    void testInboxUnreadCounter() {
        notificationDispatcherService.dispatchPaymentReceived(ownerUser, testRestaurant, "499.00", "INV-1001");
        notificationDispatcherService.dispatchQrGenerated(ownerUser, testRestaurant, "VIP Table #1");

        long unread = notificationService.getUnreadCount(ownerUser.getId());
        assertEquals(2, unread);
    }

    @Test
    @DisplayName("3. Mark Read & Read All: Mark single notification and all notifications as read")
    void testMarkReadAndReadAll() {
        notificationDispatcherService.dispatchPaymentReceived(ownerUser, testRestaurant, "100.00", "INV-101");
        notificationDispatcherService.dispatchQrGenerated(ownerUser, testRestaurant, "Bar Table");

        Page<Notification> page = notificationService.getUserNotifications(ownerUser.getId(), PageRequest.of(0, 10));
        Notification firstNotif = page.getContent().get(0);

        notificationService.markAsRead(ownerUser.getId(), firstNotif.getId());
        assertEquals(1, notificationService.getUnreadCount(ownerUser.getId()));

        notificationService.markAllAsRead(ownerUser.getId());
        assertEquals(0, notificationService.getUnreadCount(ownerUser.getId()));
    }

    @Test
    @DisplayName("4. Delete Notification: Remove notification from user inbox")
    void testDeleteNotification() {
        notificationDispatcherService.dispatchOfferEnding(ownerUser, testRestaurant, "Happy Hour 50%");
        Page<Notification> page = notificationService.getUserNotifications(ownerUser.getId(), PageRequest.of(0, 10));
        Notification notif = page.getContent().get(0);

        notificationService.deleteNotification(ownerUser.getId(), notif.getId());

        Page<Notification> updatedPage = notificationService.getUserNotifications(ownerUser.getId(), PageRequest.of(0, 10));
        assertEquals(0, updatedPage.getTotalElements());
    }
}
