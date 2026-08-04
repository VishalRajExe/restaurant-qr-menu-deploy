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
    public ResponseEntity<ApiResponse<Page<SupportTicket>>> getRestaurantTickets(
            @PathVariable Long restaurantId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(supportTicketRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId, pageable)));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<TicketDetailsPayload>> getTicketDetails(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @PathVariable Long ticketId) {

        boolean isAdmin = "SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole());
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
        TicketMessage msg = supportTicketService.addMessage(ticketId, user, request.message, request.attachments, isInternal);

        return ResponseEntity.ok(ApiResponse.success("Message sent", msg));
    }


    @PatchMapping("/{ticketId}/reopen")
    public ResponseEntity<ApiResponse<SupportTicket>> reopenTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket reopened", supportTicketService.reopenTicket(ticketId)));
    }

    @PostMapping("/{ticketId}/rate")
    public ResponseEntity<ApiResponse<SupportTicket>> rateTicket(
            @PathVariable Long ticketId,
            @RequestParam int rating,
            @RequestParam(required = false) String feedback) {
        return ResponseEntity.ok(ApiResponse.success("Ticket rated and closed", supportTicketService.rateTicket(ticketId, rating, feedback)));
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

    @Data
    public static class CreateTicketRequest {
        public SupportTicket.Category category;
        public SupportTicket.Priority priority;
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
    @lombok.Builder
    public static class TicketDetailsPayload {
        private SupportTicket ticket;
        private List<TicketMessage> messages;
    }
}
