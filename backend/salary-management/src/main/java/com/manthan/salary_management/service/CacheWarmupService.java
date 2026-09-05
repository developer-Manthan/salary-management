package com.manthan.salary_management.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    private final AnalyticsService analyticsService;

    @Async("cacheWarmupExecutor")
    public void warmAnalyticsCache() {
        try {
            log.info("Starting async cache warmup...");
            long start = System.currentTimeMillis();

            // Warm the default dashboard queries (what the frontend loads on page open)
            analyticsService.getSummary("department", "avg");
            analyticsService.getTopEarners(10, "desc");
            analyticsService.getBrackets();
            analyticsService.getAvgVsMedian();

            log.info("Cache warmup completed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("Cache warmup failed (non-critical): {}", e.getMessage());
        }
    }
}
