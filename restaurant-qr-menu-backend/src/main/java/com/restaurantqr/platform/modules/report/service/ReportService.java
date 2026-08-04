package com.restaurantqr.platform.modules.report.service;

import com.restaurantqr.platform.analytics.repository.ScanEventRepository;
import com.restaurantqr.platform.audit.repository.AuditLogRepository;
import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.category.repository.CategoryRepository;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.platform.modules.offer.repository.OfferRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import com.restaurantqr.platform.modules.subscription.repository.SubscriptionRepository;
import com.restaurantqr.platform.modules.report.dto.ReportData;
import com.restaurantqr.platform.users.repository.StaffInvitationRepository;
import com.restaurantqr.platform.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final RestaurantService restaurantService;
    private final ScanEventRepository scanEventRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final StaffInvitationRepository staffInvitationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogRepository auditLogRepository;
    private final OfferRepository offerRepository;

    public enum ReportType {
        DAILY,
        MONTHLY,
        REVENUE,
        QR_SCANS,
        MENU,
        STAFF,
        SUBSCRIPTION
    }

    public ReportData generateReportData(Long restaurantId, ReportType type, LocalDate startDate, LocalDate endDate) {
        var restaurant = restaurantService.findById(restaurantId);

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toTime = end.plusDays(1).atStartOfDay();

        String dateRange = start.toString() + " to " + end.toString();

        return switch (type) {
            case DAILY -> generateDailyReport(restaurant, fromTime, toTime, dateRange);
            case MONTHLY -> generateMonthlyReport(restaurant, fromTime, toTime, dateRange);
            case REVENUE -> generateRevenueReport(restaurant, fromTime, toTime, dateRange);
            case QR_SCANS -> generateQrScansReport(restaurant, fromTime, toTime, dateRange);
            case MENU -> generateMenuReport(restaurant, dateRange);
            case STAFF -> generateStaffReport(restaurant, dateRange);
            case SUBSCRIPTION -> generateSubscriptionReport(restaurant, dateRange);
        };
    }

    private ReportData generateDailyReport(com.restaurantqr.platform.modules.restaurant.entity.Restaurant restaurant,
                                          LocalDateTime from, LocalDateTime to, String dateRange) {
        long scans = scanEventRepository.countByRestaurantIdAndCreatedAtBetween(restaurant.getId(), from, to);
        long visitors = scanEventRepository.countUniqueVisitors(restaurant.getId(), from, to);

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Total Scans Today", String.valueOf(scans));
        summary.put("Unique Visitors Today", String.valueOf(visitors));
        summary.put("Estimated Daily Spend", "₹" + (scans * 250));

        List<String> headers = List.of("Metric Name", "Today Count", "Status");
        List<List<String>> rows = List.of(
                List.of("QR Scans", String.valueOf(scans), "ACTIVE"),
                List.of("Unique Visitors", String.valueOf(visitors), "ACTIVE"),
                List.of("Active Branches", String.valueOf(branchRepository.countByRestaurantIdAndIsDeletedFalse(restaurant.getId())), "OPERATIONAL")
        );

        return ReportData.builder()
                .reportTitle("Daily Operations Summary Report")
                .restaurantName(restaurant.getName())
                .dateRange(dateRange)
                .summaryMetrics(summary)
                .headers(headers)
                .rows(rows)
                .build();
    }

    private ReportData generateMonthlyReport(com.restaurantqr.platform.modules.restaurant.entity.Restaurant restaurant,
                                            LocalDateTime from, LocalDateTime to, String dateRange) {
        long scans = scanEventRepository.countByRestaurantIdAndCreatedAtBetween(restaurant.getId(), from, to);
        long visitors = scanEventRepository.countUniqueVisitors(restaurant.getId(), from, to);

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Monthly Scans", String.valueOf(scans));
        summary.put("Monthly Unique Visitors", String.valueOf(visitors));

        List<String> headers = List.of("Category", "Value");
        List<List<String>> rows = List.of(
                List.of("Total Customer Visits", String.valueOf(scans)),
                List.of("Unique Devices", String.valueOf(visitors)),
                List.of("Subscription Tier", restaurant.getSubscriptionPlan().name())
        );

        return ReportData.builder()
                .reportTitle("Monthly Performance & Analytics Report")
                .restaurantName(restaurant.getName())
                .dateRange(dateRange)
                .summaryMetrics(summary)
                .headers(headers)
                .rows(rows)
                .build();
    }

    private ReportData generateRevenueReport(com.restaurantqr.platform.modules.restaurant.entity.Restaurant restaurant,
                                            LocalDateTime from, LocalDateTime to, String dateRange) {
        long scans = scanEventRepository.countByRestaurantIdAndCreatedAtBetween(restaurant.getId(), from, to);
        BigDecimal estRev = BigDecimal.valueOf(scans * 250L);
        BigDecimal estTax = estRev.multiply(new BigDecimal("0.18"));

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Estimated Gross Revenue", "₹" + estRev);
        summary.put("Estimated GST Tax (18%)", "₹" + estTax);

        List<String> headers = List.of("Revenue Stream", "Amount (INR)", "Tax Portion (18%)");
        List<List<String>> rows = List.of(
                List.of("Dine-In Customer Orders", "₹" + estRev, "₹" + estTax),
                List.of("Takeaway / QR Menu Sessions", "₹" + estRev.multiply(new BigDecimal("0.3")), "₹" + estTax.multiply(new BigDecimal("0.3")))
        );

        return ReportData.builder()
                .reportTitle("Revenue & Financial Breakdown Report")
                .restaurantName(restaurant.getName())
                .dateRange(dateRange)
                .summaryMetrics(summary)
                .headers(headers)
                .rows(rows)
                .build();
    }

    private ReportData generateQrScansReport(com.restaurantqr.platform.modules.restaurant.entity.Restaurant restaurant,
                                             LocalDateTime from, LocalDateTime to, String dateRange) {
        List<Object[]> topQr = scanEventRepository.topQrCodes(restaurant.getId(), from);

        List<String> headers = List.of("QR Code ID / Table", "Scan Count", "Platform Share");
        List<List<String>> rows = new ArrayList<>();

        for (Object[] r : topQr) {
            rows.add(List.of("Table / QR #" + r[0], String.valueOf(r[1]), "Primary"));
        }
        if (rows.isEmpty()) {
            rows.add(List.of("Table #1", "0", "Primary"));
        }

        return ReportData.builder()
                .reportTitle("QR Code Scans & Traffic Report")
                .restaurantName(restaurant.getName())
                .dateRange(dateRange)
                .headers(headers)
                .rows(rows)
                .build();
    }

    private ReportData generateMenuReport(com.restaurantqr.platform.modules.restaurant.entity.Restaurant restaurant, String dateRange) {
        List<MenuItem> items = menuItemRepository.findActiveByRestaurantId(restaurant.getId());

        List<String> headers = List.of("Item Name", "Category", "Price (INR)", "Food Type", "Available");
        List<List<String>> rows = items.stream()
                .map(i -> List.of(i.getName(), i.getCategory().getName(), "₹" + i.getPrice(), i.getVegNonveg().name(), i.getIsAvailable() ? "YES" : "NO"))
                .toList();

        Map<String, String> summary = Map.of(
                "Total Active Menu Items", String.valueOf(items.size()),
                "Total Categories", String.valueOf(categoryRepository.findActiveByRestaurantId(restaurant.getId()).size())
        );

        return ReportData.builder()
                .reportTitle("Menu Performance & Catalog Report")
                .restaurantName(restaurant.getName())
                .dateRange(dateRange)
                .summaryMetrics(summary)
                .headers(headers)
                .rows(rows)
                .build();
    }

    private ReportData generateStaffReport(com.restaurantqr.platform.modules.restaurant.entity.Restaurant restaurant, String dateRange) {
        long staffCount = userRepository.countByRestaurantIdAndIsDeletedFalse(restaurant.getId());

        List<String> headers = List.of("Staff ID", "Status", "Assigned Restaurant");
        List<List<String>> rows = List.of(
                List.of("Staff Team", "ACTIVE", restaurant.getName())
        );

        Map<String, String> summary = Map.of(
                "Total Staff Users", String.valueOf(staffCount),
                "Restaurant Owner", restaurant.getName() + " Admin"
        );

        return ReportData.builder()
                .reportTitle("Staff Management & Team Activity Report")
                .restaurantName(restaurant.getName())
                .dateRange(dateRange)
                .summaryMetrics(summary)
                .headers(headers)
                .rows(rows)
                .build();
    }

    private ReportData generateSubscriptionReport(com.restaurantqr.platform.modules.restaurant.entity.Restaurant restaurant, String dateRange) {
        var subs = subscriptionRepository.findByRestaurantId(restaurant.getId());

        List<String> headers = List.of("Invoice No", "Plan", "Start Date", "End Date", "Amount Paid", "Status");
        List<List<String>> rows = subs.stream()
                .map(s -> List.of(
                        s.getInvoiceNumber() != null ? s.getInvoiceNumber() : "INV-" + s.getId(),
                        s.getPlan().name(),
                        s.getStartDate().toString(),
                        s.getEndDate().toString(),
                        "₹" + s.getAmountPaid(),
                        s.getStatus().name()
                ))
                .toList();

        Map<String, String> summary = Map.of(
                "Current Active Plan", restaurant.getSubscriptionPlan().name(),
                "Trial Status", Boolean.TRUE.equals(restaurant.getIsTrial()) ? "IN TRIAL" : "PAID"
        );

        return ReportData.builder()
                .reportTitle("Subscription SaaS Billing Report")
                .restaurantName(restaurant.getName())
                .dateRange(dateRange)
                .summaryMetrics(summary)
                .headers(headers)
                .rows(rows)
                .build();
    }
}
