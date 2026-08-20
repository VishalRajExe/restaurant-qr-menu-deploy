package com.restaurantqr.platform.modules.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long id;
    private Long restaurantId;
    private Long senderId;
    private String senderName;
    private String senderRole;
    private Long receiverId;
    private String receiverName;
    private String receiverRole;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
