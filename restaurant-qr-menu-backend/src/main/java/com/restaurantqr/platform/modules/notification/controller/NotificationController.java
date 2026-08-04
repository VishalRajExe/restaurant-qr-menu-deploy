package com.restaurantqr.platform.modules.notification.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.notification.entity.Notification;
import com.restaurantqr.platform.modules.notification.service.NotificationService;
import com.restaurantqr.platform.security.JwtUserDetails;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationInboxPayload>> getUserNotifications(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            Pageable pageable) {
        Page<Notification> page = notificationService.getUserNotifications(currentUser.getUserId(), pageable);
        long unreadCount = notificationService.getUnreadCount(currentUser.getUserId());

        var payload = NotificationInboxPayload.builder()
                .unreadCount(unreadCount)
                .notifications(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal JwtUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(currentUser.getUserId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Notification>> markAsRead(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read",
                notificationService.markAsRead(currentUser.getUserId(), id)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal JwtUserDetails currentUser) {
        notificationService.markAllAsRead(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long id) {
        notificationService.deleteNotification(currentUser.getUserId(), id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }


    @Data
    @Builder
    public static class NotificationInboxPayload {
        private long unreadCount;
        private List<Notification> notifications;
        private long totalElements;
        private int totalPages;
    }
}
