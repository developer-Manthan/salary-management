package com.manthan.salary_management.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class CacheEvictionService {

    @CacheEvict(value = {"analytics-summary", "analytics-top-earners", "analytics-brackets", "analytics-avg-vs-median"}, allEntries = true)
    public void evictAllAnalyticsCache() {
        // Cache evicted
    }
}
