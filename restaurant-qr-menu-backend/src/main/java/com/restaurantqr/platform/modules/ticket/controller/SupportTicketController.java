package com.restaurantqr.platform.modules.ticket.controller;

import com.restaurantqr.platform.common.ApiResponse;
import com.restaurantqr.platform.modules.ticket.entity.KnowledgeArticle;
import com.restaurantqr.platform.modules.ticket.entity.SupportTicket;
import com.restaurantqr.platform.modules.ticket.entity.TicketMessage;
import com.restaurantqr.platform.modules.ticket.repository.KnowledgeArticleRepository;
import com.restaurantqr.platform.modules.ticket.repository.SupportTicketRepository;
import com.restaurantqr.platform.modules.ticket.service.SupportTicketService;
import com.restaurantqr.platform.security.JwtUserDetails;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final SupportTicketRepository supportTicketRepository;
    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final com.restaurantqr.platform.users.repository.UserRepository userRepository;

    // ─── AUTHENTICATED TICKET CREATION (OWNER / CHEF) ───────────────────────────
    @PostMapping("/restaurants/{restaurantId}")
    public ResponseEntity<ApiResponse<SupportTicket>> createTicket(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long restaurantId,
            @RequestBody CreateTicketRequest request) {

        var user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new com.restaurantqr.platform.common.ResourceNotFoundException("User", currentUser.getUserId()));

        SupportTicket ticket = supportTicketService.createTicket(
                restaurantId, user, request.category, request.priority, request.subject, request.description, request.attachments);

        return ResponseEntity.ok(ApiResponse.success("Support ticket created", ticket));
    }

    @GetMapping("/restaurants/{restaurantId}")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getRestaurantTickets(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success(supportTicketRepository.findByRestaurantIdAndIsDeletedFalseOrderByCreatedAtDesc(restaurantId)));
    }

    // ─── PUBLIC CUSTOMER TICKET & REPORT ENDPOINTS ──────────────────────────────
    @PostMapping("/public/restaurants/{restaurantId}")
    public ResponseEntity<ApiResponse<SupportTicket>> createCustomerTicket(
            @PathVariable Long restaurantId,
            @RequestBody CreateCustomerTicketRequest request) {

        SupportTicket ticket = supportTicketService.createCustomerTicket(
                restaurantId, request.customerName, request.customerMobile, request.customerEmail,
                request.category, request.subject, request.description, request.attachments);

        return ResponseEntity.ok(ApiResponse.success("Dining report/ticket created successfully", ticket));
    }

    @GetMapping("/public/track")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> trackCustomerTickets(
            @RequestParam Long restaurantId,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String ticketNumber) {

        if (ticketNumber != null && !ticketNumber.isBlank()) {
            var single = supportTicketRepository.findByTicketNumber(ticketNumber.trim());
            return ResponseEntity.ok(ApiResponse.success(single.map(List::of).orElse(List.of())));
        }

        if (mobile != null && !mobile.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(
                    supportTicketRepository.findByRestaurantIdAndCustomerMobileAndIsDeletedFalseOrderByCreatedAtDesc(restaurantId, mobile.trim())));
        }

        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/public/{ticketId}")
    public ResponseEntity<ApiResponse<TicketDetailsPayload>> getPublicTicketDetails(@PathVariable Long ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId).orElse(null);
        List<TicketMessage> messages = supportTicketService.getMessagesForUser(ticketId, false);
        return ResponseEntity.ok(ApiResponse.success(TicketDetailsPayload.builder().ticket(ticket).messages(messages).build()));
    }

    @PostMapping("/public/{ticketId}/messages")
    public ResponseEntity<ApiResponse<TicketMessage>> addPublicCustomerMessage(
            @PathVariable Long ticketId,
            @RequestBody SendCustomerMessageRequest request) {

        TicketMessage msg = supportTicketService.addMessage(
                ticketId, null, request.senderName, "CUSTOMER", request.message, request.attachments, false);

        return ResponseEntity.ok(ApiResponse.success("Message sent", msg));
    }

    // ─── TICKET DETAILS & MESSAGES ──────────────────────────────────────────────
    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<TicketDetailsPayload>> getTicketDetails(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long ticketId) {

        boolean isAdmin = currentUser != null && "SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole());
        SupportTicket ticket = supportTicketRepository.findById(ticketId).orElse(null);
        List<TicketMessage> messages = supportTicketService.getMessagesForUser(ticketId, isAdmin);

        return ResponseEntity.ok(ApiResponse.success(TicketDetailsPayload.builder().ticket(ticket).messages(messages).build()));
    }

    @PostMapping("/{ticketId}/messages")
    public ResponseEntity<ApiResponse<TicketMessage>> addMessage(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long ticketId,
            @RequestBody SendMessageRequest request) {

        var user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new com.restaurantqr.platform.common.ResourceNotFoundException("User", currentUser.getUserId()));

        boolean isInternal = Boolean.TRUE.equals(request.isInternalNote) && "SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole());
        TicketMessage msg = supportTicketService.addMessage(
                ticketId, user, user.getName(), user.getRole().name(), request.message, request.attachments, isInternal);

        return ResponseEntity.ok(ApiResponse.success("Message sent", msg));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<ApiResponse<SupportTicket>> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestParam SupportTicket.Status status) {
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated", supportTicketService.updateTicketStatus(ticketId, status)));
    }

    @RequestMapping(value = "/{ticketId}/reopen", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<ApiResponse<SupportTicket>> reopenTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket reopened", supportTicketService.reopenTicket(ticketId)));
    }

    @RequestMapping(value = "/{ticketId}/rate", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<ApiResponse<SupportTicket>> rateTicket(
            @PathVariable Long ticketId,
            @RequestParam int rating,
            @RequestParam(required = false) String feedback) {
        return ResponseEntity.ok(ApiResponse.success("Ticket rated and closed", supportTicketService.rateTicket(ticketId, rating, feedback)));
    }

    @PostMapping("/{ticketId}/escalate")
    public ResponseEntity<ApiResponse<SupportTicket>> escalateTicketToAdmin(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long ticketId,
            @RequestBody(required = false) EscalateRequest request) {

        var user = currentUser != null ? userRepository.findById(currentUser.getUserId()).orElse(null) : null;
        String reason = request != null ? request.reason : null;
        SupportTicket escalated = supportTicketService.escalateTicketToAdmin(ticketId, reason, user);
        return ResponseEntity.ok(ApiResponse.success("Ticket escalated to Super Admin desk successfully", escalated));
    }

    // ─── ADMIN ENDPOINTS ────────────────────────────────────────────────────────
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getAllTicketsAdmin() {
        return ResponseEntity.ok(ApiResponse.success(supportTicketRepository.findAllByIsDeletedFalseOrderByCreatedAtDesc()));
    }

    @PatchMapping("/admin/{ticketId}/resolve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SupportTicket>> resolveTicketAdmin(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket resolved", supportTicketService.resolveTicket(ticketId)));
    }

    @GetMapping("/{ticketId}/transcript")
    public ResponseEntity<byte[]> exportTranscript(@PathVariable Long ticketId) {
        String transcript = supportTicketService.exportTranscript(ticketId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket_transcript_" + ticketId + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(transcript.getBytes());
    }

    @GetMapping("/knowledge-base/search")
    public ResponseEntity<ApiResponse<List<KnowledgeArticle>>> searchKnowledgeBase(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(knowledgeArticleRepository.searchArticles(q)));
    }

    // ─── DTOs ───────────────────────────────────────────────────────────────────
    @Data
    public static class CreateTicketRequest {
        public SupportTicket.Category category;
        public SupportTicket.Priority priority;
        public String subject;
        public String description;
        public String attachments;
    }

    @Data
    public static class CreateCustomerTicketRequest {
        public String customerName;
        public String customerMobile;
        public String customerEmail;
        public SupportTicket.Category category;
        public String subject;
        public String description;
        public String attachments;
    }

    @Data
    public static class SendMessageRequest {
        public String message;
        public String attachments;
        public Boolean isInternalNote;
    }

    @Data
    public static class SendCustomerMessageRequest {
        public String senderName;
        public String message;
        public String attachments;
    }

    @Data
    public static class EscalateRequest {
        public String reason;
    }

    @Data
    @lombok.Builder
    public static class TicketDetailsPayload {
        private SupportTicket ticket;
        private List<TicketMessage> messages;
    }
}
