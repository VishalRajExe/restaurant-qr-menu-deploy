package com.restaurantqr.platform.modules.notification.service;

import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.notification.entity.Notification;
import com.restaurantqr.platform.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);
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
