package com.manthan.salary_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePayslipResponse {
    private Long payrollCycleId;
    private String month;
    private String status;
    private LocalDateTime runAt;
    private BigDecimal baseSalary;
    private BigDecimal totalAdjustments;
    private BigDecimal finalAmount;
}
