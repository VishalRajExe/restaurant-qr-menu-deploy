package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantRequest;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.modules.ticket.entity.KnowledgeArticle;
import com.restaurantqr.platform.modules.ticket.entity.SupportTicket;
import com.restaurantqr.platform.modules.ticket.entity.TicketMessage;
import com.restaurantqr.platform.modules.ticket.repository.KnowledgeArticleRepository;
import com.restaurantqr.platform.modules.ticket.service.SupportTicketService;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@Transactional
class Phase10SupportTicketSystemTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private KnowledgeArticleRepository knowledgeArticleRepository;

    private Restaurant testRestaurant;
    private User ownerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        RestaurantRequest req = new RestaurantRequest();
        req.name = "Support Excellence Restaurant";
        req.slug = "support-rest-" + System.currentTimeMillis();
        testRestaurant = restaurantService.create(req);

        ownerUser = userRepository.save(User.builder()
                .name("Ticket Owner")
                .email("ticketowner-" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(testRestaurant)
                .build());

        adminUser = userRepository.save(User.builder()
                .name("Super Admin Agent")
                .email("adminagent-" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(User.Role.SUPER_ADMIN)
                .status(User.Status.ACTIVE)
                .build());

        JwtUserDetails details = new JwtUserDetails(ownerUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    @DisplayName("1. Ticket Creation & SLA: Generate ticket number, calculate SLA response/resolution deadlines")
    void testTicketCreationAndSLA() {
        SupportTicket ticket = supportTicketService.createTicket(
                testRestaurant.getId(),
                ownerUser,
                SupportTicket.Category.QR_PROBLEM,
                SupportTicket.Priority.CRITICAL,
                "QR Code Scanner Failure",
                "Table #4 QR code is not scanning properly on mobile devices.",
                "https://cdn.example.com/qr-screenshot.png"
        );

        assertNotNull(ticket.getId());
        assertTrue(ticket.getTicketNumber().startsWith("TICK-"));
        assertEquals(SupportTicket.Status.OPEN, ticket.getStatus());
        assertEquals(SupportTicket.Priority.CRITICAL, ticket.getPriority());
        assertNotNull(ticket.getSlaResponseDeadline());
        assertNotNull(ticket.getSlaResolutionDeadline());
    }

    @Test
    @DisplayName("2. WhatsApp-Style Chat & Internal Notes: Public customer messages vs Admin internal notes")
    void testWhatsAppStyleChatAndInternalNotes() {
        SupportTicket ticket = supportTicketService.createTicket(
                testRestaurant.getId(), ownerUser,
                SupportTicket.Category.BILLING, SupportTicket.Priority.HIGH,
                "Invoice GST Query", "Need updated GST invoice with company address.", null
        );

        // Add public message from customer
        supportTicketService.addMessage(ticket.getId(), ownerUser, "Please check ASAP.", null, false);

        // Add internal note from Super Admin
        supportTicketService.addMessage(ticket.getId(), adminUser, "Internal note: Check Razorpay GST sync.", null, true);

        // Customer sees 2 messages (initial + customer reply, internal note hidden)
        List<TicketMessage> customerView = supportTicketService.getMessagesForUser(ticket.getId(), false);
        assertEquals(2, customerView.size());

        // Admin sees 3 messages (including internal note)
        List<TicketMessage> adminView = supportTicketService.getMessagesForUser(ticket.getId(), true);
        assertEquals(3, adminView.size());
    }

    @Test
    @DisplayName("3. Assignment & Escalation: Assign team/agent and escalate to Level 2")
    void testAssignmentAndEscalation() {
        SupportTicket ticket = supportTicketService.createTicket(
                testRestaurant.getId(), ownerUser,
                SupportTicket.Category.TECHNICAL_ISSUE, SupportTicket.Priority.HIGH,
                "API Gateway Timeout", "504 gateway timeout on menu update API.", null
        );

        SupportTicket assigned = supportTicketService.assignTicket(ticket.getId(), adminUser.getId(), SupportTicket.Team.DEVELOPER);
        assertEquals(SupportTicket.Status.ASSIGNED, assigned.getStatus());
        assertEquals(adminUser.getId(), assigned.getAssignedToUser().getId());

        SupportTicket escalated = supportTicketService.escalateTicket(ticket.getId(), SupportTicket.EscalationLevel.LEVEL_2);
        assertEquals(SupportTicket.EscalationLevel.LEVEL_2, escalated.getEscalationLevel());
    }

    @Test
    @DisplayName("4. Customer Rating & Reopen: Resolve ticket, rate 5 stars, and reopen ticket")
    void testCustomerRatingAndReopen() {
        SupportTicket ticket = supportTicketService.createTicket(
                testRestaurant.getId(), ownerUser,
                SupportTicket.Category.MENU_ISSUE, SupportTicket.Priority.MEDIUM,
                "Category Display Order", "Reordering categories is not saving.", null
        );

        supportTicketService.resolveTicket(ticket.getId());

        SupportTicket rated = supportTicketService.rateTicket(ticket.getId(), 5, "Fixed fast, excellent support!");
        assertEquals(5, rated.getRating());
        assertEquals(SupportTicket.Status.CLOSED, rated.getStatus());

        SupportTicket reopened = supportTicketService.reopenTicket(ticket.getId());
        assertEquals(SupportTicket.Status.OPEN, reopened.getStatus());

        String transcript = supportTicketService.exportTranscript(ticket.getId());
        assertTrue(transcript.contains("Category Display Order"));
    }

    @Test
    @DisplayName("5. Knowledge Base & Admin Dashboard: Search FAQ articles and fetch Super Admin KPIs")
    void testKnowledgeBaseAndAdminDashboard() {
        knowledgeArticleRepository.save(KnowledgeArticle.builder()
                .title("How to Generate Table QR Codes")
                .slug("generate-qr-codes")
                .category("QR_CODES")
                .content("Go to Branches -> QR Codes -> Generate Table QR.")
                .build());

        List<KnowledgeArticle> results = knowledgeArticleRepository.searchArticles("Table QR");
        assertEquals(1, results.size());

        SupportTicketService.SupportAdminDashboardDto stats = supportTicketService.getAdminDashboardStats();
        assertNotNull(stats);
        assertTrue(stats.getSlaCompliancePercent() > 0);
    }
}
