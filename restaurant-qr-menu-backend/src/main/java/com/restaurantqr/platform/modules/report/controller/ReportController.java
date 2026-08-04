package com.restaurantqr.platform.modules.report.controller;

import com.restaurantqr.platform.modules.report.dto.ReportData;
import com.restaurantqr.platform.modules.report.service.ReportExportService;
import com.restaurantqr.platform.modules.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public enum ExportFormat {
        CSV,
        EXCEL,
        PDF
    }

    @GetMapping("/restaurants/{restaurantId}/export")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN') or hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "DAILY") ReportService.ReportType type,
            @RequestParam(defaultValue = "PDF") ExportFormat format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ReportData data = reportService.generateReportData(restaurantId, type, startDate, endDate);

        byte[] body;
        String contentType;
        String extension;

        switch (format) {
            case CSV -> {
                body = reportExportService.exportCsv(data);
                contentType = "text/csv; charset=UTF-8";
                extension = "csv";
            }
            case EXCEL -> {
                body = reportExportService.exportExcel(data);
                contentType = "application/vnd.ms-excel";
                extension = "xls";
            }
            default -> {
                body = reportExportService.exportPdf(data);
                contentType = "application/pdf";
                extension = "pdf";
            }
        }

        String filename = "report_" + type.name().toLowerCase() + "_" + restaurantId + "." + extension;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
