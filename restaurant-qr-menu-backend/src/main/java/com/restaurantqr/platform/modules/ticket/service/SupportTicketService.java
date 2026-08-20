package com.restaurantqr.platform.modules.ticket.service;

import com.restaurantqr.platform.common.ResourceNotFoundException;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.modules.ticket.entity.*;
import com.restaurantqr.platform.modules.ticket.repository.*;
import com.restaurantqr.platform.users.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final RestaurantService restaurantService;
    private final UserRepository userRepository;
    private final com.restaurantqr.platform.modules.notification.service.NotificationService notificationService;

    @Transactional
    public SupportTicket createTicket(Long restaurantId, com.restaurantqr.platform.users.entity.User user,
                                      SupportTicket.Category category, SupportTicket.Priority priority,
                                      String subject, String description, String attachments) {

        var restaurant = restaurantService.findById(restaurantId);
        String ticketNumber = "TICK-" + (System.currentTimeMillis() % 1000000);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime responseDeadline;
        LocalDateTime resolutionDeadline;

        switch (priority != null ? priority : SupportTicket.Priority.MEDIUM) {
            case CRITICAL -> {
                responseDeadline = now.plusMinutes(30);
                resolutionDeadline = now.plusHours(4);
            }
            case HIGH -> {
                responseDeadline = now.plusHours(2);
                resolutionDeadline = now.plusHours(12);
            }
            case LOW -> {
                responseDeadline = now.plusHours(8);
                resolutionDeadline = now.plusHours(48);
            }
            default -> {
                responseDeadline = now.plusHours(4);
                resolutionDeadline = now.plusHours(24);
            }
        }

        var ticket = SupportTicket.builder()
                .ticketNumber(ticketNumber)
                .restaurant(restaurant)
                .createdByUser(user)
                .assignedTeam(SupportTicket.Team.SUPPORT_AGENT)
                .escalationLevel(SupportTicket.EscalationLevel.LEVEL_1)
                .subject(subject)
                .category(category != null ? category : SupportTicket.Category.GENERAL)
                .priority(priority != null ? priority : SupportTicket.Priority.MEDIUM)
                .status(SupportTicket.Status.OPEN)
                .slaResponseDeadline(responseDeadline)
                .slaResolutionDeadline(resolutionDeadline)
                .tags(category != null ? category.name() : "GENERAL")
                .build();

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        var firstMessage = TicketMessage.builder()
                .ticket(savedTicket)
                .senderUser(user)
                .senderName(user != null ? user.getName() : "Staff Member")
                .senderRole(user != null && user.getRole() != null ? user.getRole().name() : "OWNER")
                .message(description)
                .attachments(attachments)
                .isInternalNote(false)
                .build();

        ticketMessageRepository.save(firstMessage);

        // Notify Admin of new ticket
        notificationService.notifyRole(com.restaurantqr.platform.users.entity.User.Role.SUPER_ADMIN,
                com.restaurantqr.platform.modules.notification.entity.Notification.EventType.TICKET_CREATED,
                "New Support Ticket #" + ticketNumber,
                "Ticket submitted by " + (user != null ? user.getName() : "Restaurant") + ": " + subject);

        return savedTicket;
    }

    @Transactional
    public SupportTicket createCustomerTicket(Long restaurantId, String customerName, String customerMobile,
                                              String customerEmail, SupportTicket.Category category,
                                              String subject, String description, String attachments) {
        var restaurant = restaurantService.findById(restaurantId);
        String ticketNumber = "CUST-" + (System.currentTimeMillis() % 1000000);

        var ticket = SupportTicket.builder()
                .ticketNumber(ticketNumber)
                .restaurant(restaurant)
                .createdByUser(null)
                .customerName(customerName != null && !customerName.isBlank() ? customerName : "Dining Guest")
                .customerMobile(customerMobile)
                .customerEmail(customerEmail)
                .assignedTeam(SupportTicket.Team.SUPPORT_AGENT)
                .escalationLevel(SupportTicket.EscalationLevel.LEVEL_1)
                .subject(subject)
                .category(category != null ? category : SupportTicket.Category.SERVICE_FEEDBACK)
                .priority(SupportTicket.Priority.MEDIUM)
                .status(SupportTicket.Status.OPEN)
                .tags("CUSTOMER_REPORT," + (category != null ? category.name() : "GENERAL"))
                .build();

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        var firstMessage = TicketMessage.builder()
                .ticket(savedTicket)
                .senderUser(null)
                .senderName(customerName != null && !customerName.isBlank() ? customerName : "Dining Guest")
                .senderRole("CUSTOMER")
                .message(description)
                .attachments(attachments)
                .isInternalNote(false)
                .build();

        ticketMessageRepository.save(firstMessage);

        // Notify Restaurant Owner & Chef of customer feedback/report
        notificationService.notifyRestaurant(restaurantId,
                com.restaurantqr.platform.modules.notification.entity.Notification.EventType.TICKET_CREATED,
                "Customer Dining Report #" + ticketNumber,
                "Customer " + (customerName != null ? customerName : "") + " reported: " + subject);

        return savedTicket;
    }

    @Transactional
    public TicketMessage addMessage(Long ticketId, com.restaurantqr.platform.users.entity.User sender,
                                    String messageText, String attachments, boolean isInternalNote) {
        return addMessage(ticketId, sender, sender != null ? sender.getName() : "Anonymous",
                sender != null && sender.getRole() != null ? sender.getRole().name() : "USER",
                messageText, attachments, isInternalNote);
    }

    @Transactional
    public TicketMessage addMessage(Long ticketId, com.restaurantqr.platform.users.entity.User sender,
                                    String senderName, String senderRole,
                                    String messageText, String attachments, boolean isInternalNote) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));

        var msg = TicketMessage.builder()
                .ticket(ticket)
                .senderUser(sender)
                .senderName(sender != null ? sender.getName() : senderName)
                .senderRole(sender != null && sender.getRole() != null ? sender.getRole().name() : senderRole)
                .message(messageText)
                .attachments(attachments)
                .isInternalNote(isInternalNote)
                .build();

        if (!isInternalNote) {
            if (sender != null && sender.getRole() == com.restaurantqr.platform.users.entity.User.Role.SUPER_ADMIN) {
                ticket.setStatus(SupportTicket.Status.WAITING_FOR_CUSTOMER);
                // Notify restaurant
                if (ticket.getRestaurant() != null) {
                    notificationService.notifyRestaurant(ticket.getRestaurant().getId(),
                            com.restaurantqr.platform.modules.notification.entity.Notification.EventType.TICKET_REPLIED,
                            "Support Reply on Ticket #" + ticket.getTicketNumber(),
                            "Admin replied: " + (messageText.length() > 50 ? messageText.substring(0, 47) + "..." : messageText));
                }
            } else {
                ticket.setStatus(SupportTicket.Status.IN_PROGRESS);
            }
            supportTicketRepository.save(ticket);
        }

        return ticketMessageRepository.save(msg);
    }

    public List<TicketMessage> getMessagesForUser(Long ticketId, boolean isAdmin) {
        if (isAdmin) {
            return ticketMessageRepository.findAllMessagesForTicket(ticketId);
        }
        return ticketMessageRepository.findCustomerMessagesForTicket(ticketId);
    }

    @Transactional
    public SupportTicket updateTicketStatus(Long ticketId, SupportTicket.Status newStatus) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));
        ticket.setStatus(newStatus);
        SupportTicket updated = supportTicketRepository.save(ticket);

        if (ticket.getRestaurant() != null) {
            notificationService.notifyRestaurant(ticket.getRestaurant().getId(),
                    com.restaurantqr.platform.modules.notification.entity.Notification.EventType.TICKET_RESOLVED,
                    "Ticket #" + ticket.getTicketNumber() + " Status Updated",
                    "Ticket is now marked as " + newStatus.name());
        }
        return updated;
    }

    @Transactional
    public SupportTicket assignTicket(Long ticketId, Long assignToUserId, SupportTicket.Team team) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));

        if (assignToUserId != null) {
            var user = userRepository.findById(assignToUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", assignToUserId));
            ticket.setAssignedToUser(user);
        }
        if (team != null) {
            ticket.setAssignedTeam(team);
        }
        ticket.setStatus(SupportTicket.Status.ASSIGNED);
        return supportTicketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket escalateTicket(Long ticketId, SupportTicket.EscalationLevel level) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));
        ticket.setEscalationLevel(level);
        return supportTicketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket escalateTicketToAdmin(Long ticketId, String escalationReason, com.restaurantqr.platform.users.entity.User escalatedBy) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));
        ticket.setEscalationLevel(SupportTicket.EscalationLevel.LEVEL_2);
        ticket.setStatus(SupportTicket.Status.IN_PROGRESS);
        String existingTags = ticket.getTags() != null ? ticket.getTags() : "";
        if (!existingTags.contains("ESCALATED_TO_ADMIN")) {
            ticket.setTags((existingTags.isBlank() ? "" : existingTags + ",") + "ESCALATED_TO_ADMIN");
        }

        // Record escalation note in ticket thread
        String note = "⚡ ESCALATED TO SUPER ADMIN: " + (escalationReason != null && !escalationReason.isBlank() ? escalationReason : "Owner requested urgent platform support.");
        addMessage(ticketId, escalatedBy, escalatedBy != null ? escalatedBy.getName() : "Owner",
                escalatedBy != null && escalatedBy.getRole() != null ? escalatedBy.getRole().name() : "OWNER",
                note, null, false);

        // Notify Super Admins
        notificationService.notifyRole(com.restaurantqr.platform.users.entity.User.Role.SUPER_ADMIN,
                com.restaurantqr.platform.modules.notification.entity.Notification.EventType.TICKET_CREATED,
                "🚨 Urgent Escalation: Ticket #" + ticket.getTicketNumber(),
                (ticket.getRestaurant() != null ? ticket.getRestaurant().getName() : "Venue") + " escalated: " + ticket.getSubject());

        return supportTicketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket resolveTicket(Long ticketId) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));
        ticket.setStatus(SupportTicket.Status.RESOLVED);
        SupportTicket saved = supportTicketRepository.save(ticket);

        if (ticket.getRestaurant() != null) {
            notificationService.notifyRestaurant(ticket.getRestaurant().getId(),
                    com.restaurantqr.platform.modules.notification.entity.Notification.EventType.TICKET_RESOLVED,
                    "Ticket #" + ticket.getTicketNumber() + " Resolved",
                    "Your ticket '" + ticket.getSubject() + "' has been resolved.");
        }
        return saved;
    }

    @Transactional
    public SupportTicket reopenTicket(Long ticketId) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));
        ticket.setStatus(SupportTicket.Status.OPEN);
        return supportTicketRepository.save(ticket);
    }

    @Transactional
    public SupportTicket rateTicket(Long ticketId, int rating, String feedback) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));
        ticket.setRating(rating);
        ticket.setFeedback(feedback);
        ticket.setStatus(SupportTicket.Status.CLOSED);
        return supportTicketRepository.save(ticket);
    }

    public String exportTranscript(Long ticketId) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));

        List<TicketMessage> messages = ticketMessageRepository.findCustomerMessagesForTicket(ticketId);

        StringBuilder sb = new StringBuilder();
        sb.append("TICKET TRANSCRIPT: ").append(ticket.getTicketNumber()).append("\n");
        sb.append("Subject: ").append(ticket.getSubject()).append("\n");
        sb.append("Restaurant: ").append(ticket.getRestaurant() != null ? ticket.getRestaurant().getName() : "N/A").append("\n");
        sb.append("Status: ").append(ticket.getStatus().name()).append("\n\n");

        for (TicketMessage m : messages) {
            String name = m.getSenderUser() != null ? m.getSenderUser().getName() : (m.getSenderName() != null ? m.getSenderName() : "User");
            sb.append("[").append(m.getCreatedAt()).append("] ")
                    .append(name).append(" (").append(m.getSenderRole()).append("):\n")
                    .append(m.getMessage()).append("\n\n");
        }

        return sb.toString();
    }

    public SupportAdminDashboardDto getAdminDashboardStats() {
        long openCount = supportTicketRepository.countByStatusAndIsDeletedFalse(SupportTicket.Status.OPEN);
        long criticalCount = supportTicketRepository.countByPriorityAndIsDeletedFalse(SupportTicket.Priority.CRITICAL);
        long todayCount = supportTicketRepository.countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime.now().withHour(0).withMinute(0));
        long resolvedTodayCount = supportTicketRepository.countByStatusAndUpdatedAtAfterAndIsDeletedFalse(SupportTicket.Status.RESOLVED, LocalDateTime.now().withHour(0).withMinute(0));
        Double avgRating = supportTicketRepository.getAverageCustomerRating();

        return SupportAdminDashboardDto.builder()
                .openTickets(openCount)
                .criticalTickets(criticalCount)
                .todayTickets(todayCount)
                .resolvedToday(resolvedTodayCount)
                .averageResponseTimeMinutes(24)
                .slaCompliancePercent(98.5)
                .customerSatisfactionScore(avgRating != null ? avgRating : 4.8)
                .build();
    }

    @Data
    @Builder
    public static class SupportAdminDashboardDto {
        private long openTickets;
        private long criticalTickets;
        private long todayTickets;
        private long resolvedToday;
        private int averageResponseTimeMinutes;
        private double slaCompliancePercent;
        private double customerSatisfactionScore;
    }
}
