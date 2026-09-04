package com.manthan.salary_management.service;

import com.manthan.salary_management.dto.response.AnalyticsSummaryResponse;
import com.manthan.salary_management.dto.response.AvgVsMedianResponse;
import com.manthan.salary_management.dto.response.BracketResponse;
import com.manthan.salary_management.dto.response.TopEarnerResponse;
import com.manthan.salary_management.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsSummaryResponse getSummary(String dimension, String metric) {
        String dimLower = dimension.toLowerCase();
        String metLower = metric.toLowerCase();
        if (!List.of("department", "country", "jobtitle", "status").contains(dimLower)) {
            throw new IllegalArgumentException("Invalid dimension: " + dimension + ". Valid: department, country, jobTitle, status");
        }
        if (!List.of("sum", "avg", "median", "min", "max", "count", "shareoftotal").contains(metLower)) {
            throw new IllegalArgumentException("Invalid metric: " + metric + ". Valid: sum, avg, median, min, max, count, shareOfTotal");
        }

        List<Object[]> results = analyticsRepository.getSummary(dimension, metric);
        List<AnalyticsSummaryResponse.DimensionMetricEntry> entries = results.stream()
                .map(row -> AnalyticsSummaryResponse.DimensionMetricEntry.builder()
                        .label(row[0] != null ? row[0].toString() : "Unknown")
                        .value(toBigDecimal(row[1]))
                        .build())
                .collect(Collectors.toList());

        return AnalyticsSummaryResponse.builder()
                .dimension(dimension)
                .metric(metric)
                .data(entries)
                .build();
    }

    public List<TopEarnerResponse> getTopEarners(int n, String order) {
        if (n <= 0) n = 10;
        if (!"asc".equalsIgnoreCase(order) && !"desc".equalsIgnoreCase(order)) {
            order = "desc";
        }

        List<Object[]> results = analyticsRepository.getTopEarners(n, order);
        return results.stream()
                .map(row -> TopEarnerResponse.builder()
                        .employeeId(((Number) row[0]).longValue())
                        .employeeCode((String) row[1])
                        .name((String) row[2])
                        .department((String) row[3])
                        .jobTitle((String) row[4])
                        .country((String) row[5])
                        .currentSalary(toBigDecimal(row[6]))
                        .build())
                .collect(Collectors.toList());
    }

    public BracketResponse getBrackets() {
        List<Object[]> results = analyticsRepository.getBrackets();
        
        long totalCount = results.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        List<BracketResponse.BracketEntry> entries = results.stream()
                .map(row -> {
                    long count = ((Number) row[1]).longValue();
                    BigDecimal percentage = totalCount == 0 ? BigDecimal.ZERO : 
                            BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(100))
                                    .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
                    
                    return BracketResponse.BracketEntry.builder()
                            .range((String) row[0])
                            .count(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());

        return BracketResponse.builder()
                .brackets(entries)
                .build();
    }

    public AvgVsMedianResponse getAvgVsMedian() {
        Object[] result = analyticsRepository.getAvgVsMedian();
        BigDecimal average = toBigDecimal(result[0]);
        BigDecimal median = toBigDecimal(result[1]);
        BigDecimal diff = average.subtract(median).abs();
        
        String skew;
        if (average.compareTo(median) > 0) skew = "RIGHT";
        else if (average.compareTo(median) < 0) skew = "LEFT";
        else skew = "SYMMETRIC";

        return AvgVsMedianResponse.builder()
                .average(average)
                .median(median)
                .difference(diff)
                .skewDirection(skew)
                .build();
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }
}
