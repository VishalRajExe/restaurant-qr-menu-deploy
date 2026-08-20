package com.restaurantqr.platform.modules.notification.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notif_user_read", columnList = "user_id, is_read"),
                @Index(name = "idx_notif_restaurant", columnList = "restaurant_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = true)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private Channel channel = Channel.IN_APP;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public enum EventType {
        NEW_ORDER,
        ORDER_STATUS_CHANGED,
        ORDER_READY,
        ORDER_CANCELLED,
        MENU_UPDATED,
        CATEGORY_CREATED,
        QR_GENERATED,
        PAYMENT_RECEIVED,
        SUBSCRIPTION_EXPIRING,
        OFFER_ENDING,
        NEW_STAFF_JOINED,
        TICKET_CREATED,
        TICKET_REPLIED,
        TICKET_RESOLVED,
        SUPPORT_TICKET_UPDATE,
        SECURITY_ALERT,
        CHAT_MESSAGE,
        SYSTEM_NOTICE
    }

    public enum Channel {
        EMAIL,
        IN_APP,
        SMS,
        PUSH
    }

    public void markRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
