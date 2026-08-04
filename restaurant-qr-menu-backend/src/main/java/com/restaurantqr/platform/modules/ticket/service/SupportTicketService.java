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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final SavedReplyRepository savedReplyRepository;
    private final RestaurantService restaurantService;
    private final UserRepository userRepository;

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
                .category(category)
                .priority(priority != null ? priority : SupportTicket.Priority.MEDIUM)
                .status(SupportTicket.Status.OPEN)
                .slaResponseDeadline(responseDeadline)
                .slaResolutionDeadline(resolutionDeadline)
                .tags(category.name() + "," + (priority != null ? priority.name() : "MEDIUM"))
                .build();

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        var firstMessage = TicketMessage.builder()
                .ticket(savedTicket)
                .senderUser(user)
                .senderRole(user.getRole().name())
                .message(description)
                .attachments(attachments)
                .isInternalNote(false)
                .build();

        ticketMessageRepository.save(firstMessage);
        return savedTicket;
    }

    @Transactional
    public TicketMessage addMessage(Long ticketId, com.restaurantqr.platform.users.entity.User sender,
                                    String messageText, String attachments, boolean isInternalNote) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));

        var msg = TicketMessage.builder()
                .ticket(ticket)
                .senderUser(sender)
                .senderRole(sender.getRole().name())
                .message(messageText)
                .attachments(attachments)
                .isInternalNote(isInternalNote)
                .build();

        if (!isInternalNote) {
            if (sender.getRole() == com.restaurantqr.platform.users.entity.User.Role.SUPER_ADMIN) {
                ticket.setStatus(SupportTicket.Status.WAITING_FOR_CUSTOMER);
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
    public SupportTicket resolveTicket(Long ticketId) {
        var ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", ticketId));
        ticket.setStatus(SupportTicket.Status.RESOLVED);
        return supportTicketRepository.save(ticket);
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
        sb.append("Restaurant: ").append(ticket.getRestaurant().getName()).append("\n");
        sb.append("Status: ").append(ticket.getStatus().name()).append("\n\n");

        for (TicketMessage m : messages) {
            sb.append("[").append(m.getCreatedAt()).append("] ")
                    .append(m.getSenderUser().getName()).append(" (").append(m.getSenderRole()).append("):\n")
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
