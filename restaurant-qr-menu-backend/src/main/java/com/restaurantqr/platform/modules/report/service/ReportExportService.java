package com.restaurantqr.platform.modules.report.service;

import com.restaurantqr.platform.modules.report.dto.ReportData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportExportService {

    public byte[] exportCsv(ReportData data) {
        StringBuilder sb = new StringBuilder();
        // Title & Summary
        sb.append("\"").append(data.getReportTitle()).append("\"\n");
        sb.append("\"Restaurant: ").append(data.getRestaurantName()).append("\"\n");
        sb.append("\"Period: ").append(data.getDateRange()).append("\"\n\n");

        if (data.getSummaryMetrics() != null && !data.getSummaryMetrics().isEmpty()) {
            sb.append("\"--- SUMMARY METRICS ---\"\n");
            for (Map.Entry<String, String> entry : data.getSummaryMetrics().entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\",\"").append(entry.getValue()).append("\"\n");
            }
            sb.append("\n");
        }

        // Headers
        if (data.getHeaders() != null && !data.getHeaders().isEmpty()) {
            for (int i = 0; i < data.getHeaders().size(); i++) {
                sb.append("\"").append(data.getHeaders().get(i).replace("\"", "\"\"")).append("\"");
                if (i < data.getHeaders().size() - 1) sb.append(",");
            }
            sb.append("\n");
        }

        // Rows
        if (data.getRows() != null) {
            for (List<String> row : data.getRows()) {
                for (int i = 0; i < row.size(); i++) {
                    String val = row.get(i) != null ? row.get(i).replace("\"", "\"\"") : "";
                    sb.append("\"").append(val).append("\"");
                    if (i < row.size() - 1) sb.append(",");
                }
                sb.append("\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportExcel(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<?mso-application progid=\"Excel.Sheet\"?>\n");
        sb.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n");
        sb.append(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n");
        sb.append(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n");
        sb.append(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");
        sb.append(" <Worksheet ss:Name=\"Report\">\n");
        sb.append("  <Table>\n");

        // Title row
        sb.append("   <Row><Cell><Data ss:Type=\"String\">").append(escapeXml(data.getReportTitle())).append("</Data></Cell></Row>\n");
        sb.append("   <Row><Cell><Data ss:Type=\"String\">Restaurant: ").append(escapeXml(data.getRestaurantName())).append("</Data></Cell></Row>\n");
        sb.append("   <Row><Cell><Data ss:Type=\"String\">Period: ").append(escapeXml(data.getDateRange())).append("</Data></Cell></Row>\n");
        sb.append("   <Row></Row>\n");

        // Summary
        if (data.getSummaryMetrics() != null) {
            for (Map.Entry<String, String> entry : data.getSummaryMetrics().entrySet()) {
                sb.append("   <Row>")
                        .append("<Cell><Data ss:Type=\"String\">").append(escapeXml(entry.getKey())).append("</Data></Cell>")
                        .append("<Cell><Data ss:Type=\"String\">").append(escapeXml(entry.getValue())).append("</Data></Cell>")
                        .append("</Row>\n");
            }
            sb.append("   <Row></Row>\n");
        }

        // Headers
        if (data.getHeaders() != null) {
            sb.append("   <Row>\n");
            for (String h : data.getHeaders()) {
                sb.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(h)).append("</Data></Cell>\n");
            }
            sb.append("   </Row>\n");
        }

        // Rows
        if (data.getRows() != null) {
            for (List<String> row : data.getRows()) {
                sb.append("   <Row>\n");
                for (String val : row) {
                    sb.append("    <Cell><Data ss:Type=\"String\">").append(escapeXml(val != null ? val : "")).append("</Data></Cell>\n");
                }
                sb.append("   </Row>\n");
            }
        }

        sb.append("  </Table>\n");
        sb.append(" </Worksheet>\n");
        sb.append("</Workbook>\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(ReportData data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            // PDF Magic Header & Content Generator
            baos.write("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));
            baos.write("%âãÏÓ\n".getBytes(StandardCharsets.UTF_8));
            baos.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.UTF_8));
            baos.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.UTF_8));

            StringBuilder text = new StringBuilder();
            text.append("BT /F1 16 Tf 50 750 Td (").append(escapePdf(data.getReportTitle())).append(") Tj ET\n");
            text.append("BT /F1 12 Tf 50 730 Td (Restaurant: ").append(escapePdf(data.getRestaurantName())).append(") Tj ET\n");
            text.append("BT /F1 10 Tf 50 715 Td (Period: ").append(escapePdf(data.getDateRange())).append(") Tj ET\n");

            int y = 680;
            if (data.getSummaryMetrics() != null) {
                for (Map.Entry<String, String> entry : data.getSummaryMetrics().entrySet()) {
                    text.append("BT /F1 10 Tf 50 ").append(y).append(" Td (").append(escapePdf(entry.getKey() + ": " + entry.getValue())).append(") Tj ET\n");
                    y -= 15;
                    if (y < 100) break;
                }
            }

            y -= 10;
            if (data.getHeaders() != null) {
                text.append("BT /F1 10 Tf 50 ").append(y).append(" Td (").append(escapePdf(String.join(" | ", data.getHeaders()))).append(") Tj ET\n");
                y -= 15;
            }

            if (data.getRows() != null) {
                for (List<String> row : data.getRows()) {
                    text.append("BT /F1 9 Tf 50 ").append(y).append(" Td (").append(escapePdf(String.join(" | ", row))).append(") Tj ET\n");
                    y -= 12;
                    if (y < 50) break;
                }
            }

            byte[] contentBytes = text.toString().getBytes(StandardCharsets.UTF_8);

            baos.write(String.format("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> >> >> >>\nendobj\n").getBytes(StandardCharsets.UTF_8));
            baos.write(String.format("4 0 obj\n<< /Length %d >>\nstream\n", contentBytes.length).getBytes(StandardCharsets.UTF_8));
            baos.write(contentBytes);
            baos.write("\nendstream\nendobj\n".getBytes(StandardCharsets.UTF_8));
            baos.write("xref\n0 5\n0000000000 65535 f \n0000000015 00000 n \n0000000068 00000 n \n0000000125 00000 n \n0000000300 00000 n \ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n400\n%%EOF\n".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to generate PDF: {}", e.getMessage());
        }
        return baos.toByteArray();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escapePdf(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
