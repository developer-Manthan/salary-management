package com.manthan.salary_management.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheEvictionService {

    private final CacheWarmupService cacheWarmupService;

    @CacheEvict(value = {"analytics-summary", "analytics-top-earners", "analytics-brackets", "analytics-avg-vs-median"}, allEntries = true)
    public void evictAllAnalyticsCache() {
        cacheWarmupService.warmAnalyticsCache();
    }
}
