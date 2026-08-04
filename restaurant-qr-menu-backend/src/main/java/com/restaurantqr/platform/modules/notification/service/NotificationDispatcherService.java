package com.restaurantqr.platform.modules.notification.service;

import com.restaurantqr.platform.config.EmailService;
import com.restaurantqr.platform.modules.notification.entity.Notification;
import com.restaurantqr.platform.modules.notification.repository.NotificationRepository;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.users.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcherService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public void dispatch(User user, Restaurant restaurant, Notification.EventType eventType,
                         String title, String message, List<Notification.Channel> channels) {

        List<Notification.Channel> targetChannels = (channels != null && !channels.isEmpty())
                ? channels
                : List.of(Notification.Channel.IN_APP, Notification.Channel.EMAIL);

        for (Notification.Channel channel : targetChannels) {
            switch (channel) {
                case IN_APP -> {
                    Notification notif = Notification.builder()
                            .user(user)
                            .restaurant(restaurant)
                            .eventType(eventType)
                            .channel(Notification.Channel.IN_APP)
                            .title(title)
                            .message(message)
                            .build();
                    notificationRepository.save(notif);
                }
                case EMAIL -> {
                    try {
                        emailService.sendSimpleEmail(user.getEmail(), title, message);
                    } catch (Exception e) {
                        log.warn("Email notification send failed for user {}: {}", user.getEmail(), e.getMessage());
                    }
                }

                case SMS -> log.info("Simulating SMS dispatch to user {}: [{}] {}", user.getEmail(), title, message);
                case PUSH -> log.info("Simulating PUSH notification dispatch to user {}: [{}] {}", user.getEmail(), title, message);
            }
        }
    }

    public void dispatchSubscriptionExpiring(User user, Restaurant restaurant, int daysLeft) {
        String title = "Subscription Expiring Soon!";
        String message = String.format("Your restaurant '%s' subscription plan will expire in %d days. Please renew to avoid service disruption.", restaurant.getName(), daysLeft);
        dispatch(user, restaurant, Notification.EventType.SUBSCRIPTION_EXPIRING, title, message, List.of(Notification.Channel.IN_APP, Notification.Channel.EMAIL, Notification.Channel.PUSH));
    }

    public void dispatchOfferEnding(User user, Restaurant restaurant, String offerTitle) {
        String title = "Promotional Offer Ending";
        String message = String.format("Offer '%s' for '%s' is ending today.", offerTitle, restaurant.getName());
        dispatch(user, restaurant, Notification.EventType.OFFER_ENDING, title, message, List.of(Notification.Channel.IN_APP, Notification.Channel.EMAIL));
    }

    public void dispatchNewStaffJoined(User owner, Restaurant restaurant, String staffName) {
        String title = "New Staff Member Joined";
        String message = String.format("Staff member '%s' has accepted the invitation and joined %s.", staffName, restaurant.getName());
        dispatch(owner, restaurant, Notification.EventType.NEW_STAFF_JOINED, title, message, List.of(Notification.Channel.IN_APP, Notification.Channel.EMAIL, Notification.Channel.SMS));
    }

    public void dispatchQrGenerated(User user, Restaurant restaurant, String qrDetails) {
        String title = "New QR Code Generated";
        String message = String.format("A new QR code (%s) was generated for %s.", qrDetails, restaurant.getName());
        dispatch(user, restaurant, Notification.EventType.QR_GENERATED, title, message, List.of(Notification.Channel.IN_APP));
    }

    public void dispatchPaymentReceived(User user, Restaurant restaurant, String amount, String invoiceNo) {
        String title = "Payment Received - Invoice #" + invoiceNo;
        String message = String.format("Payment of ₹%s for %s was successfully processed.", amount, restaurant.getName());
        dispatch(user, restaurant, Notification.EventType.PAYMENT_RECEIVED, title, message, List.of(Notification.Channel.IN_APP, Notification.Channel.EMAIL, Notification.Channel.SMS, Notification.Channel.PUSH));
    }
}
