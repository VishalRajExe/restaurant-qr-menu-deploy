package com.restaurantqr.platform.modules.chat.repository;

import com.restaurantqr.platform.modules.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.restaurant.id = :restaurantId AND " +
           "((m.sender.id = :u1 AND m.receiver.id = :u2) OR (m.sender.id = :u2 AND m.receiver.id = :u1)) " +
           "AND m.isDeleted = false ORDER BY m.createdAt ASC")
    List<ChatMessage> findThreadMessages(@Param("restaurantId") Long restaurantId,
                                         @Param("u1") Long u1,
                                         @Param("u2") Long u2);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.restaurant.id = :restaurantId AND " +
           "m.receiver.id = :receiverId AND m.isRead = false AND m.isDeleted = false")
    long countUnreadMessagesForUser(@Param("restaurantId") Long restaurantId,
                                    @Param("receiverId") Long receiverId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.restaurant.id = :restaurantId AND " +
           "m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.isRead = false AND m.isDeleted = false")
    long countUnreadMessagesBetween(@Param("restaurantId") Long restaurantId,
                                    @Param("senderId") Long senderId,
                                    @Param("receiverId") Long receiverId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.restaurant.id = :restaurantId AND " +
           "m.sender.id = :senderId AND m.receiver.id = :receiverId AND m.isRead = false")
    void markThreadAsRead(@Param("restaurantId") Long restaurantId,
                          @Param("senderId") Long senderId,
                          @Param("receiverId") Long receiverId);

    @Query("SELECT m FROM ChatMessage m WHERE m.restaurant.id = :restaurantId AND " +
           "(m.sender.id = :userId OR m.receiver.id = :userId) AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessagesForUser(@Param("restaurantId") Long restaurantId,
                                                @Param("userId") Long userId);
}
