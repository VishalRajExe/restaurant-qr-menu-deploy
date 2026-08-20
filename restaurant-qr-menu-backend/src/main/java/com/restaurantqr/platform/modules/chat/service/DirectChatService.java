package com.restaurantqr.platform.modules.chat.service;

import com.restaurantqr.platform.common.BadRequestException;
import com.restaurantqr.platform.common.ForbiddenException;
import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.chat.dto.ChatContactDto;
import com.restaurantqr.platform.modules.chat.dto.ChatMessageDto;
import com.restaurantqr.platform.modules.chat.dto.SendMessageRequest;
import com.restaurantqr.platform.modules.chat.entity.ChatMessage;
import com.restaurantqr.platform.modules.chat.repository.ChatMessageRepository;
import com.restaurantqr.platform.modules.notification.service.NotificationService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.repository.RestaurantRepository;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DirectChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final NotificationService notificationService;

    public List<ChatContactDto> getContacts(Long restaurantId, String currentUsername) {
        User currentUser = findUserByEmail(currentUsername);
        validateUserInRestaurant(currentUser, restaurantId);

        List<User> restaurantUsers = userRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId);
        List<ChatContactDto> contacts = new ArrayList<>();

        for (User user : restaurantUsers) {
            if (user.getId().equals(currentUser.getId())) continue;

            // Find last message between these two
            List<ChatMessage> thread = chatMessageRepository.findThreadMessages(restaurantId, currentUser.getId(), user.getId());
            String lastMsg = null;
            java.time.LocalDateTime lastTime = null;
            if (!thread.isEmpty()) {
                ChatMessage last = thread.get(thread.size() - 1);
                lastMsg = last.getMessage();
                lastTime = last.getCreatedAt();
            }

            long unread = chatMessageRepository.countUnreadMessagesBetween(restaurantId, user.getId(), currentUser.getId());

            contacts.add(ChatContactDto.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .role(user.getRole() == User.Role.RESTAURANT_OWNER ? "OWNER" : "CHEF")
                    .avatarUrl(user.getProfileImageUrl())
                    .lastMessage(lastMsg)
                    .lastMessageTime(lastTime)
                    .unreadCount(unread)
                    .isOnline(true)
                    .build());
        }

        return contacts;
    }

    @Transactional
    public List<ChatMessageDto> getThread(Long restaurantId, String currentUsername, Long otherUserId) {
        User currentUser = findUserByEmail(currentUsername);
        validateUserInRestaurant(currentUser, restaurantId);

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + otherUserId));
        validateUserInRestaurant(otherUser, restaurantId);

        // Mark unread messages sent by otherUser to currentUser as read
        chatMessageRepository.markThreadAsRead(restaurantId, otherUserId, currentUser.getId());

        List<ChatMessage> messages = chatMessageRepository.findThreadMessages(restaurantId, currentUser.getId(), otherUserId);
        return messages.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto sendMessage(Long restaurantId, String currentUsername, SendMessageRequest request) {
        User sender = findUserByEmail(currentUsername);
        validateUserInRestaurant(sender, restaurantId);

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found: " + request.getReceiverId()));
        validateUserInRestaurant(receiver, restaurantId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        String senderRoleStr = sender.getRole() == User.Role.RESTAURANT_OWNER ? "OWNER" : "CHEF";
        String receiverRoleStr = receiver.getRole() == User.Role.RESTAURANT_OWNER ? "OWNER" : "CHEF";

        ChatMessage msg = ChatMessage.builder()
                .restaurant(restaurant)
                .sender(sender)
                .receiver(receiver)
                .senderName(sender.getName())
                .senderRole(senderRoleStr)
                .receiverName(receiver.getName())
                .receiverRole(receiverRoleStr)
                .message(request.getMessage().trim())
                .isRead(false)
                .build();

        ChatMessage saved = chatMessageRepository.save(msg);
        log.info("Direct chat message sent from userId={} to userId={} in restaurantId={}",
                sender.getId(), receiver.getId(), restaurantId);

        // Real-time notification for receiver
        try {
            notificationService.createNotification(
                    receiver,
                    restaurantId,
                    com.restaurantqr.platform.modules.notification.entity.Notification.EventType.CHAT_MESSAGE,
                    "New message from " + sender.getName(),
                    request.getMessage().trim()
            );
        } catch (Exception e) {
            log.debug("Notification push notice: {}", e.getMessage());
        }

        return mapToDto(saved);
    }

    @Transactional
    public void markThreadAsRead(Long restaurantId, String currentUsername, Long otherUserId) {
        User currentUser = findUserByEmail(currentUsername);
        validateUserInRestaurant(currentUser, restaurantId);
        chatMessageRepository.markThreadAsRead(restaurantId, otherUserId, currentUser.getId());
    }

    public long getUnreadCount(Long restaurantId, String currentUsername) {
        User currentUser = findUserByEmail(currentUsername);
        validateUserInRestaurant(currentUser, restaurantId);
        return chatMessageRepository.countUnreadMessagesForUser(restaurantId, currentUser.getId());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + email));
    }

    private void validateUserInRestaurant(User user, Long restaurantId) {
        if (user.getRestaurant() == null || !user.getRestaurant().getId().equals(restaurantId)) {
            // Super admins can view any restaurant chat
            if (user.getRole() != User.Role.SUPER_ADMIN) {
                throw new ForbiddenException("Access denied: User does not belong to restaurant " + restaurantId);
            }
        }
    }

    private ChatMessageDto mapToDto(ChatMessage m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .restaurantId(m.getRestaurant().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSenderName() != null ? m.getSenderName() : m.getSender().getName())
                .senderRole(m.getSenderRole() != null ? m.getSenderRole() : "STAFF")
                .receiverId(m.getReceiver().getId())
                .receiverName(m.getReceiverName() != null ? m.getReceiverName() : m.getReceiver().getName())
                .receiverRole(m.getReceiverRole() != null ? m.getReceiverRole() : "STAFF")
                .message(m.getMessage())
                .isRead(m.getIsRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
