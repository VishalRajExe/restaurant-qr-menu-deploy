package com.restaurantqr.platform.modules.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportData {

    private String reportTitle;
    private String restaurantName;
    private String dateRange;
    private Map<String, String> summaryMetrics;
    private List<String> headers;
    private List<List<String>> rows;
}
