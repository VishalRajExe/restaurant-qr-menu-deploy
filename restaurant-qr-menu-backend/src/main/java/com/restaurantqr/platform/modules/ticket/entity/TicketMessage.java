package com.restaurantqr.platform.modules.ticket.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ticket_messages",
        indexes = @Index(name = "idx_msg_ticket", columnList = "ticket_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User senderUser;

    @Column(name = "sender_role", nullable = false, length = 50)
    private String senderRole;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments; // Comma-separated media URLs (Images, PDF, Videos, Logs)

    @Column(name = "is_internal_note", nullable = false)
    @Builder.Default
    private Boolean isInternalNote = false;
}
