package com.manthan.salary_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollCycleSummaryResponse {
    private long totalEmployees;
    private BigDecimal totalPayout;
    private BigDecimal totalAdjustments;
}
