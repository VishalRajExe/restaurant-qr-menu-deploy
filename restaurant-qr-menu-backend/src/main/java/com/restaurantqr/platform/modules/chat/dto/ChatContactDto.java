package com.restaurantqr.platform.modules.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatContactDto {
    private Long userId;
    private String name;
    private String email;
    private String role; // "OWNER" | "CHEF" | "STAFF"
    private String avatarUrl;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private boolean isOnline;
}
