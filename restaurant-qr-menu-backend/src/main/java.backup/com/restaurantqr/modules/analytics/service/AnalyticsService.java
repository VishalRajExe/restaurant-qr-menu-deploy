package com.restaurantqr.modules.analytics.service;

import com.restaurantqr.modules.analytics.entity.ScanEvent;
import com.restaurantqr.modules.analytics.repository.ScanEventRepository;
import com.restaurantqr.modules.qr.entity.QrCode;
import com.restaurantqr.modules.qr.repository.QrCodeRepository;
import com.restaurantqr.modules.restaurant.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ScanEventRepository scanEventRepository;
    private final QrCodeRepository qrCodeRepository;
    private final RestaurantService restaurantService;

    // ─── Record a scan (called on QR resolve) ─────────────────────────────────

    @Async
    @Transactional
    public void recordScan(QrCode qrCode, HttpServletRequest request) {
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

    // ─── Dashboard analytics ──────────────────────────────────────────────────

    public DashboardStats getDashboardStats(Long restaurantId) {
        restaurantService.findById(restaurantId); // assert exists

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

    // ─── Device detection ─────────────────────────────────────────────────────

    private ScanEvent.DeviceType detectDeviceType(String userAgent) {
        if (userAgent == null) return ScanEvent.DeviceType.UNKNOWN;
        String ua = userAgent.toLowerCase();
        if (ua.contains("tablet") || ua.contains("ipad")) return ScanEvent.DeviceType.TABLET;
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone"))
            return ScanEvent.DeviceType.MOBILE;
        return ScanEvent.DeviceType.DESKTOP;
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
