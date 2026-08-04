package com.restaurantqr.platform.modules.ticket.entity;

import com.restaurantqr.platform.common.BaseEntity;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets",
        indexes = {
                @Index(name = "idx_ticket_restaurant", columnList = "restaurant_id"),
                @Index(name = "idx_ticket_status", columnList = "status"),
                @Index(name = "idx_ticket_number", columnList = "ticket_number")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket extends BaseEntity {

    @Column(name = "ticket_number", nullable = false, unique = true, length = 50)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedToUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_team", length = 50)
    @Builder.Default
    private Team assignedTeam = Team.SUPPORT_AGENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", length = 30)
    @Builder.Default
    private EscalationLevel escalationLevel = EscalationLevel.LEVEL_1;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 30)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.OPEN;

    @Column(name = "sla_response_deadline")
    private LocalDateTime slaResponseDeadline;

    @Column(name = "sla_resolution_deadline")
    private LocalDateTime slaResolutionDeadline;

    @Column(name = "is_sla_violated", nullable = false)
    @Builder.Default
    private Boolean isSlaViolated = false;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "tags")
    private String tags;

    public enum Category {
        BILLING,
        SUBSCRIPTION,
        TECHNICAL_ISSUE,
        QR_PROBLEM,
        MENU_ISSUE,
        FEATURE_REQUEST,
        BUG_REPORT,
        OTHER
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Status {
        OPEN,
        ASSIGNED,
        IN_PROGRESS,
        WAITING_FOR_CUSTOMER,
        RESOLVED,
        CLOSED
    }

    public enum Team {
        DEVELOPER,
        SUPPORT_AGENT,
        BILLING_TEAM,
        SALES_TEAM
    }

    public enum EscalationLevel {
        LEVEL_1,
        LEVEL_2,
        DEVELOPER,
        MANAGER
    }
}
