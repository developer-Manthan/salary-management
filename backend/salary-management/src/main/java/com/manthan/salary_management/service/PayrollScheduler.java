package com.manthan.salary_management.service;

import com.manthan.salary_management.entity.enums.TriggerType;
import com.manthan.salary_management.exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollScheduler {

    private final PayrollService payrollService;

    @Scheduled(cron = "0 0 0 1 * *") // 1st of each month at midnight
    public void runMonthlyPayroll() {
        String currentMonth = YearMonth.now().toString(); // YYYY-MM
        log.info("Scheduled payroll run triggered for month: {}", currentMonth);
        try {
            payrollService.runPayroll(currentMonth, TriggerType.SCHEDULED);
            log.info("Scheduled payroll run completed for month: {}", currentMonth);
        } catch (DuplicateResourceException e) {
            log.warn("Payroll already run for month {}: {}", currentMonth, e.getMessage());
        } catch (Exception e) {
            log.error("Scheduled payroll run failed for month {}: {}", currentMonth, e.getMessage(), e);
        }
    }
}
