package com.manthan.salary_management.controller;

import com.manthan.salary_management.dto.response.AnalyticsSummaryResponse;
import com.manthan.salary_management.dto.response.AvgVsMedianResponse;
import com.manthan.salary_management.dto.response.BracketResponse;
import com.manthan.salary_management.dto.response.TopEarnerResponse;
import com.manthan.salary_management.service.AnalyticsService;
import com.manthan.salary_management.service.CacheEvictionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CacheEvictionService cacheEvictionService;

    @GetMapping("/summary")
    public AnalyticsSummaryResponse getSummary(
            @RequestParam String dimension,
            @RequestParam String metric) {
        return analyticsService.getSummary(dimension, metric);
    }

    @GetMapping("/top-earners")
    public List<TopEarnerResponse> getTopEarners(
            @RequestParam(defaultValue = "10") int n,
            @RequestParam(defaultValue = "desc") String order) {
        return analyticsService.getTopEarners(n, order);
    }

    @GetMapping("/brackets")
    public BracketResponse getBrackets() {
        return analyticsService.getBrackets();
    }

    @GetMapping("/avg-vs-median")
    public AvgVsMedianResponse getAvgVsMedian() {
        return analyticsService.getAvgVsMedian();
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        cacheEvictionService.evictAllAnalyticsCache();
        return ResponseEntity.ok(Map.of("message", "Analytics cache cleared"));
    }
}
