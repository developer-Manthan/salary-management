package com.manthan.salary_management.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AnalyticsSummaryResponse {
    private String dimension;
    private String metric;
    private List<DimensionMetricEntry> data;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DimensionMetricEntry {
        private String label;
        private BigDecimal value;
    }
}
