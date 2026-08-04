package com.restaurantqr.platform.modules;

import com.restaurantqr.platform.RestaurantQrApplication;
import com.restaurantqr.platform.modules.report.controller.ReportController;
import com.restaurantqr.platform.modules.report.dto.ReportData;
import com.restaurantqr.platform.modules.report.service.ReportExportService;
import com.restaurantqr.platform.modules.report.service.ReportService;
import com.restaurantqr.platform.modules.restaurant.entity.Restaurant;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantRequest;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.security.JwtUserDetails;
import com.restaurantqr.platform.users.entity.User;
import com.restaurantqr.platform.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RestaurantQrApplication.class)
@ActiveProfiles("test")
@Transactional
class Phase8ReportsTest {

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportExportService reportExportService;

    @Autowired
    private ReportController reportController;

    private Restaurant testRestaurant;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        RestaurantRequest req = new RestaurantRequest();
        req.name = "Reports & Analytics Bistro";
        req.slug = "reports-bistro-" + System.currentTimeMillis();
        testRestaurant = restaurantService.create(req);

        ownerUser = userRepository.save(User.builder()
                .name("Reports Owner")
                .email("repowner-" + System.currentTimeMillis() + "@test.com")
                .password("password123")
                .role(User.Role.RESTAURANT_OWNER)
                .status(User.Status.ACTIVE)
                .restaurant(testRestaurant)
                .build());

        JwtUserDetails details = new JwtUserDetails(ownerUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }

    @Test
    @DisplayName("1. CSV Export: Export Revenue & QR Scans report to CSV format")
    void testCsvExport() {
        ResponseEntity<byte[]> response = reportController.exportReport(
                testRestaurant.getId(),
                ReportService.ReportType.REVENUE,
                ReportController.ExportFormat.CSV,
                null, null);

        assertNotNull(response.getBody());
        assertTrue(response.getHeaders().getContentType().toString().startsWith("text/csv"));

        String csvText = new String(response.getBody(), StandardCharsets.UTF_8);

        assertTrue(csvText.contains("Revenue"));
        assertTrue(csvText.contains("Dine-In Customer Orders"));
    }

    @Test
    @DisplayName("2. Excel Export: Export Menu Performance & Staff report to XML/XLS format")
    void testExcelExport() {
        ResponseEntity<byte[]> response = reportController.exportReport(
                testRestaurant.getId(),
                ReportService.ReportType.MENU,
                ReportController.ExportFormat.EXCEL,
                null, null);

        assertNotNull(response.getBody());
        assertEquals("application/vnd.ms-excel", response.getHeaders().getContentType().toString());

        String xmlText = new String(response.getBody(), StandardCharsets.UTF_8);
        assertTrue(xmlText.contains("<Workbook"));
        assertTrue(xmlText.contains("Menu Performance"));
    }

    @Test
    @DisplayName("3. PDF Export: Export Daily & Subscription report to PDF format")
    void testPdfExport() {
        ResponseEntity<byte[]> response = reportController.exportReport(
                testRestaurant.getId(),
                ReportService.ReportType.DAILY,
                ReportController.ExportFormat.PDF,
                null, null);

        assertNotNull(response.getBody());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());

        String pdfText = new String(response.getBody(), StandardCharsets.UTF_8);
        assertTrue(pdfText.startsWith("%PDF-1.4"));
    }

    @Test
    @DisplayName("4. All 7 Report Types Aggregation: Verify DAILY, MONTHLY, REVENUE, QR_SCANS, MENU, STAFF, SUBSCRIPTION")
    void testAllReportTypesAggregation() {
        for (ReportService.ReportType type : ReportService.ReportType.values()) {
            ReportData data = reportService.generateReportData(testRestaurant.getId(), type, null, null);
            assertNotNull(data);
            assertNotNull(data.getReportTitle());
            assertNotNull(data.getRestaurantName());
            assertNotNull(data.getHeaders());
            assertNotNull(data.getRows());
        }
    }
}
