package com.restaurantqr.platform.modules.ticket.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.ticket.entity.SavedReply;
import com.restaurantqr.platform.modules.ticket.entity.SupportTicket;
import com.restaurantqr.platform.modules.ticket.repository.SavedReplyRepository;
import com.restaurantqr.platform.modules.ticket.repository.SupportTicketRepository;
import com.restaurantqr.platform.modules.ticket.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/tickets")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;
    private final SupportTicketRepository supportTicketRepository;
    private final SavedReplyRepository savedReplyRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<SupportTicketService.SupportAdminDashboardDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(supportTicketService.getAdminDashboardStats()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupportTicket>>> getAllTickets(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(supportTicketRepository.findAllTickets(pageable)));
    }

    @RequestMapping(value = "/{ticketId}/assign", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<ApiResponse<SupportTicket>> assignTicket(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Long assignToUserId,
            @RequestParam(required = false) SupportTicket.Team team) {
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned", supportTicketService.assignTicket(ticketId, assignToUserId, team)));
    }

    @RequestMapping(value = "/{ticketId}/escalate", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<ApiResponse<SupportTicket>> escalateTicket(
            @PathVariable Long ticketId,
            @RequestParam SupportTicket.EscalationLevel level) {
        return ResponseEntity.ok(ApiResponse.success("Ticket escalated", supportTicketService.escalateTicket(ticketId, level)));
    }

    @RequestMapping(value = "/{ticketId}/resolve", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<ApiResponse<SupportTicket>> resolveTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket resolved", supportTicketService.resolveTicket(ticketId)));
    }

    @GetMapping("/saved-replies")
    public ResponseEntity<ApiResponse<List<SavedReply>>> getSavedReplies() {
        return ResponseEntity.ok(ApiResponse.success(savedReplyRepository.findByIsDeletedFalse()));
    }

    @GetMapping("/sla-overdue")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getSlaOverdueTickets() {
        return ResponseEntity.ok(ApiResponse.success(supportTicketRepository.findSlaOverdueTickets()));
    }
}
