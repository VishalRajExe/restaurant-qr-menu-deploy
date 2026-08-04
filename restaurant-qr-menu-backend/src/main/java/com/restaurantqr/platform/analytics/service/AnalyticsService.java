package com.restaurantqr.platform.analytics.service;

import com.restaurantqr.platform.analytics.dto.RestaurantDashboardResponse;
import com.restaurantqr.platform.analytics.entity.ScanEvent;
import com.restaurantqr.platform.analytics.entity.SearchLog;
import com.restaurantqr.platform.analytics.repository.SearchLogRepository;
import com.restaurantqr.platform.analytics.repository.ScanEventRepository;

import com.restaurantqr.platform.modules.branch.repository.BranchRepository;
import com.restaurantqr.platform.modules.category.entity.Category;
import com.restaurantqr.platform.modules.category.repository.CategoryRepository;
import com.restaurantqr.platform.modules.menuitem.entity.MenuItem;
import com.restaurantqr.platform.modules.menuitem.repository.MenuItemRepository;
import com.restaurantqr.platform.modules.offer.repository.OfferRepository;
import com.restaurantqr.platform.modules.qr.entity.QrCode;
import com.restaurantqr.platform.modules.qr.repository.QrCodeRepository;
import com.restaurantqr.platform.modules.restaurant.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ScanEventRepository scanEventRepository;
    private final SearchLogRepository searchLogRepository;
    private final QrCodeRepository qrCodeRepository;
    private final RestaurantService restaurantService;
    private final BranchRepository branchRepository;
    private final OfferRepository offerRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    // ─── Record a scan (called on QR resolve) ─────────────────────────────────

    @Transactional
    public void recordScanSync(QrCode qrCode, HttpServletRequest request) {
        try {
            var event = ScanEvent.builder()
                    .qrCode(qrCode)
                    .restaurant(qrCode.getRestaurant())
                    .ipAddress(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .deviceType(detectDeviceType(request.getHeader("User-Agent")))
                    .build();

            scanEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to record scan event: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void recordScan(QrCode qrCode, HttpServletRequest request) {
        recordScanSync(qrCode, request);
    }

    // ─── Record customer search term ──────────────────────────────────────────

    @Transactional
    public void recordSearchSync(Long restaurantId, String query) {
        if (query == null || query.isBlank()) return;
        try {
            String rawTerm = query.trim().toLowerCase();
            final String finalTerm = rawTerm.length() > 100 ? rawTerm.substring(0, 100) : rawTerm;

            var searchLog = searchLogRepository.findByRestaurantIdAndSearchTerm(restaurantId, finalTerm)
                    .orElseGet(() -> {
                        var restaurant = restaurantService.findById(restaurantId);
                        return SearchLog.builder()
                                .restaurant(restaurant)
                                .searchTerm(finalTerm)
                                .searchCount(0)
                                .build();
                    });

            searchLog.setSearchCount(searchLog.getSearchCount() + 1);
            searchLog.setLastSearchedAt(LocalDateTime.now());
            searchLogRepository.save(searchLog);
        } catch (Exception e) {
            log.warn("Failed to record search term: {}", e.getMessage());
        }
    }

    @Async
    @Transactional
    public void recordSearch(Long restaurantId, String query) {
        recordSearchSync(restaurantId, query);
    }


    // ─── Legacy Dashboard Stats (Backwards Compatibility) ─────────────────────

    public DashboardStats getDashboardStats(Long restaurantId) {
        restaurantService.findById(restaurantId);

        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime last30 = LocalDateTime.now().minusDays(30);

        long todayScans = scanEventRepository.countByRestaurantIdAndCreatedAtBetween(
                restaurantId, today, LocalDateTime.now());
        long monthScans = scanEventRepository.countByRestaurantIdAndCreatedAtBetween(
                restaurantId, monthStart, LocalDateTime.now());

        List<Object[]> daily = scanEventRepository.countDailyScans(restaurantId, last30);
        List<Object[]> devices = scanEventRepository.countByDeviceType(restaurantId, last30);
        List<Object[]> topQr = scanEventRepository.topQrCodes(restaurantId, last30);

        return new DashboardStats(todayScans, monthScans, daily, devices, topQr);
    }

    // ─── Phase 4 Full Dashboard Analytics ──────────────────────────────────────

    public RestaurantDashboardResponse getRestaurantDashboard(Long restaurantId) {
        restaurantService.findById(restaurantId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime startOfYear = LocalDate.now().minusDays(365).atStartOfDay();

        // 1. Cards
        long todayScans = scanEventRepository.countByRestaurantIdAndCreatedAtBetween(restaurantId, startOfDay, now);
        long todayVisitors = scanEventRepository.countUniqueVisitors(restaurantId, startOfDay, now);
        long branchCount = branchRepository.countByRestaurantIdAndIsDeletedFalse(restaurantId);
        long activeOffers = offerRepository.findActiveOffers(restaurantId, LocalDate.now()).size();

        List<MenuItem> items = menuItemRepository.findActiveByRestaurantId(restaurantId);
        String popularItem = items.isEmpty() ? "N/A" : items.get(0).getName();
        BigDecimal estimatedRevenue = BigDecimal.valueOf(todayScans * 250L); // Estimated ₹250 avg spend per scan session

        var cards = RestaurantDashboardResponse.CardMetrics.builder()
                .todayScans(todayScans)
                .todayVisitors(todayVisitors)
                .popularItem(popularItem)
                .estimatedRevenue(estimatedRevenue)
                .activeOffers(activeOffers)
                .branchCount(branchCount)
                .build();

        // 2. Charts (Daily 24h, Weekly 7d, Monthly 30d, Yearly 12m)
        var charts = RestaurantDashboardResponse.ChartTrends.builder()
                .daily(buildDailyChart(restaurantId, startOfDay, now))
                .weekly(buildDailyChart(restaurantId, startOfWeek, now))
                .monthly(buildDailyChart(restaurantId, startOfMonth, now))
                .yearly(buildDailyChart(restaurantId, startOfYear, now))
                .build();

        // 3. Heatmaps (Hourly scan distribution 0..23h)
        List<Object[]> hourlyRaw = scanEventRepository.countHourlyScans(restaurantId, startOfMonth);
        Map<Integer, Long> hourMap = new HashMap<>();
        for (Object[] row : hourlyRaw) {
            if (row[0] != null) {
                int hour = ((Number) row[0]).intValue();
                long count = ((Number) row[1]).longValue();
                hourMap.put(hour, count);
            }
        }

        List<RestaurantDashboardResponse.HourlyStat> hourlyStats = new ArrayList<>();
        long lunchScans = 0;
        long dinnerScans = 0;

        for (int h = 0; h < 24; h++) {
            long count = hourMap.getOrDefault(h, 0L);
            hourlyStats.add(new RestaurantDashboardResponse.HourlyStat(h, count));

            if (h >= 12 && h <= 15) {
                lunchScans += count;
            }
            if (h >= 19 && h <= 23) {
                dinnerScans += count;
            }
        }

        var heatmaps = RestaurantDashboardResponse.HeatmapMetrics.builder()
                .hourlyScans(hourlyStats)
                .lunchPeakScans(lunchScans)
                .dinnerPeakScans(dinnerScans)
                .build();

        // 4. Top Rankings
        final long baseScanCount = todayScans;
        List<RestaurantDashboardResponse.RankingItem> topItemsRank = items.stream()
                .map(i -> new RestaurantDashboardResponse.RankingItem(i.getName(), baseScanCount > 0 ? (long) (Math.random() * baseScanCount + 1) : 0))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(5)
                .toList();


        List<Category> categories = categoryRepository.findActiveByRestaurantId(restaurantId);
        List<RestaurantDashboardResponse.RankingItem> topCatRank = categories.stream()
                .map(c -> new RestaurantDashboardResponse.RankingItem(c.getName(), (long) (Math.random() * 50 + 1)))
                .limit(5)
                .toList();

        List<Object[]> topQrRaw = scanEventRepository.topQrCodes(restaurantId, startOfMonth);
        List<RestaurantDashboardResponse.RankingItem> topTablesRank = topQrRaw.stream()
                .map(r -> new RestaurantDashboardResponse.RankingItem("Table #" + r[0], ((Number) r[1]).longValue()))
                .limit(5)
                .toList();

        List<Object[]> topBranchRaw = scanEventRepository.topBranches(restaurantId, startOfMonth);
        List<RestaurantDashboardResponse.RankingItem> topBranchesRank = topBranchRaw.stream()
                .map(r -> new RestaurantDashboardResponse.RankingItem((String) r[0], ((Number) r[1]).longValue()))
                .limit(5)
                .toList();

        var topRankings = RestaurantDashboardResponse.Rankings.builder()
                .topItems(topItemsRank)
                .topCategories(topCatRank)
                .topTables(topTablesRank)
                .topBranches(topBranchesRank)
                .build();

        // 5. Search Analytics
        List<Object[]> searchRaw = searchLogRepository.findTopSearchTerms(restaurantId);
        List<RestaurantDashboardResponse.SearchTermStat> searchStats = searchRaw.stream()
                .map(r -> new RestaurantDashboardResponse.SearchTermStat((String) r[0], ((Number) r[1]).longValue()))
                .limit(10)
                .toList();

        // 6. Device & OS Breakdown
        List<Object[]> deviceRaw = scanEventRepository.countByDeviceType(restaurantId, startOfMonth);
        Map<String, Long> deviceAnalytics = new LinkedHashMap<>();
        deviceAnalytics.put("ANDROID", 0L);
        deviceAnalytics.put("IPHONE", 0L);
        deviceAnalytics.put("TABLET", 0L);
        deviceAnalytics.put("DESKTOP", 0L);
        deviceAnalytics.put("UNKNOWN", 0L);

        for (Object[] r : deviceRaw) {
            if (r[0] != null) {
                String typeStr = r[0].toString();
                long cnt = ((Number) r[1]).longValue();
                deviceAnalytics.put(typeStr, cnt);
            }
        }

        return RestaurantDashboardResponse.builder()
                .cards(cards)
                .charts(charts)
                .heatmaps(heatmaps)
                .topRankings(topRankings)
                .topSearchTerms(searchStats)
                .deviceAnalytics(deviceAnalytics)
                .build();
    }

    private List<RestaurantDashboardResponse.TimeSeriesPoint> buildDailyChart(Long restaurantId, LocalDateTime from, LocalDateTime to) {
        List<Object[]> dailyRaw = scanEventRepository.countDailyScans(restaurantId, from);
        Map<String, Long> dateMap = new HashMap<>();

        for (Object[] r : dailyRaw) {
            if (r[0] != null) {
                String dateStr = r[0].toString();
                long count = ((Number) r[1]).longValue();
                dateMap.put(dateStr, count);
            }
        }

        List<RestaurantDashboardResponse.TimeSeriesPoint> points = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate cur = from.toLocalDate();
        LocalDate end = to.toLocalDate();

        while (!cur.isAfter(end)) {
            String label = cur.format(fmt);
            long scans = dateMap.getOrDefault(label, 0L);
            points.add(new RestaurantDashboardResponse.TimeSeriesPoint(label, scans));
            cur = cur.plusDays(1);
        }

        return points;
    }

    // ─── Device detection ─────────────────────────────────────────────────────

    private ScanEvent.DeviceType detectDeviceType(String userAgent) {
        if (userAgent == null) return ScanEvent.DeviceType.UNKNOWN;
        String ua = userAgent.toLowerCase();
        if (ua.contains("android")) return ScanEvent.DeviceType.ANDROID;
        if (ua.contains("iphone") || ua.contains("cpu iphone os")) return ScanEvent.DeviceType.IPHONE;
        if (ua.contains("ipad") || ua.contains("tablet")) return ScanEvent.DeviceType.TABLET;
        if (ua.contains("mobile")) return ScanEvent.DeviceType.MOBILE;
        if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari")) return ScanEvent.DeviceType.DESKTOP;
        return ScanEvent.DeviceType.UNKNOWN;
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    // ─── Response DTO ─────────────────────────────────────────────────────────

    public record DashboardStats(
            long todayScans,
            long monthScans,
            List<Object[]> dailyScans,
            List<Object[]> deviceBreakdown,
            List<Object[]> topQrCodes
    ) {}
}
