package com.restaurantqr.platform.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDashboardResponse {

    // ─── 1. Cards ─────────────────────────────────────────────────────────────
    private CardMetrics cards;

    // ─── 2. Time-Series Trends & Charts ────────────────────────────────────────
    private ChartTrends charts;

    // ─── 3. Heatmaps ──────────────────────────────────────────────────────────
    private HeatmapMetrics heatmaps;

    // ─── 4. Top Rankings ──────────────────────────────────────────────────────
    private Rankings topRankings;

    // ─── 5. Search Analytics ──────────────────────────────────────────────────
    private List<SearchTermStat> topSearchTerms;

    // ─── 6. Device Analytics ──────────────────────────────────────────────────
    private Map<String, Long> deviceAnalytics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardMetrics {
        private long todayScans;
        private long todayVisitors;
        private String popularItem;
        private BigDecimal estimatedRevenue;
        private long activeOffers;
        private long branchCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartTrends {
        private List<TimeSeriesPoint> daily;   // 24 hours
        private List<TimeSeriesPoint> weekly;  // 7 days
        private List<TimeSeriesPoint> monthly; // 30 days
        private List<TimeSeriesPoint> yearly;  // 12 months
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapMetrics {
        private List<HourlyStat> hourlyScans; // 0..23 hours
        private long lunchPeakScans;          // 12:00 - 15:00
        private long dinnerPeakScans;         // 19:00 - 23:00
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rankings {
        private List<RankingItem> topItems;
        private List<RankingItem> topCategories;
        private List<RankingItem> topTables;
        private List<RankingItem> topBranches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private String label;
        private long scans;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyStat {
        private int hour;
        private long scans;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankingItem {
        private String name;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchTermStat {
        private String term;
        private long searchCount;
    }
}
