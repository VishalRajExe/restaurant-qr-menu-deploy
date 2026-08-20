package com.restaurantqr.platform.modules.chat.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.chat.dto.ChatContactDto;
import com.restaurantqr.platform.modules.chat.dto.ChatMessageDto;
import com.restaurantqr.platform.modules.chat.dto.SendMessageRequest;
import com.restaurantqr.platform.modules.chat.service.DirectChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/restaurants/{restaurantId}/chat")
@RequiredArgsConstructor
public class DirectChatController {

    private final DirectChatService directChatService;

    @GetMapping("/contacts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<List<ChatContactDto>>> getContacts(
            @PathVariable Long restaurantId,
            Authentication authentication) {
        String username = authentication.getName();
        List<ChatContactDto> contacts = directChatService.getContacts(restaurantId, username);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @GetMapping("/threads/{otherUserId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getThread(
            @PathVariable Long restaurantId,
            @PathVariable Long otherUserId,
            Authentication authentication) {
        String username = authentication.getName();
        List<ChatMessageDto> messages = directChatService.getThread(restaurantId, username, otherUserId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/messages")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(
            @PathVariable Long restaurantId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ChatMessageDto sent = directChatService.sendMessage(restaurantId, username, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent", sent));
    }

    @PatchMapping("/threads/{otherUserId}/read")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<Void>> markThreadRead(
            @PathVariable Long restaurantId,
            @PathVariable Long otherUserId,
            Authentication authentication) {
        String username = authentication.getName();
        directChatService.markThreadAsRead(restaurantId, username, otherUserId);
        return ResponseEntity.ok(ApiResponse.success("Thread marked as read", null));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER', 'STAFF', 'CHEF')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @PathVariable Long restaurantId,
            Authentication authentication) {
        String username = authentication.getName();
        long count = directChatService.getUnreadCount(restaurantId, username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }
}
