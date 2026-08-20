package com.restaurantqr.platform.modules.notification.service;

import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.notification.entity.Notification;
import com.restaurantqr.platform.modules.notification.repository.NotificationRepository;
import com.restaurantqr.platform.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final com.restaurantqr.platform.users.repository.UserRepository userRepository;
    private final com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository restaurantRepository;

    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);
    }

    @Transactional
    public Notification createNotification(User user, Long restaurantId, Notification.EventType eventType, String title, String message) {
        var restaurant = restaurantId != null ? restaurantRepository.findById(restaurantId).orElse(null) : null;
        if (restaurant == null && user.getRestaurant() != null) {
            restaurant = user.getRestaurant();
        }

        var notification = Notification.builder()
                .user(user)
                .restaurant(restaurant)
                .eventType(eventType)
                .channel(Notification.Channel.IN_APP)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public void notifyRestaurant(Long restaurantId, Notification.EventType eventType, String title, String message) {
        if (restaurantId == null) return;
        var users = userRepository.findAll().stream()
                .filter(u -> !u.getIsDeleted() && u.getRestaurant() != null && u.getRestaurant().getId().equals(restaurantId))
                .toList();

        for (var user : users) {
            createNotification(user, restaurantId, eventType, title, message);
        }
    }

    @Transactional
    public void notifyRole(User.Role role, Notification.EventType eventType, String title, String message) {
        var users = userRepository.findAll().stream()
                .filter(u -> !u.getIsDeleted() && u.getRole() == role)
                .toList();

        for (var user : users) {
            createNotification(user, null, eventType, title, message);
        }
    }

    @Transactional
    public Notification markAsRead(Long userId, Long notificationId) {
        var notif = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId) && !n.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notif.markRead();
        return notificationRepository.save(notif);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        var notif = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(userId) && !n.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        notif.softDelete();
        notificationRepository.save(notif);
    }
}
