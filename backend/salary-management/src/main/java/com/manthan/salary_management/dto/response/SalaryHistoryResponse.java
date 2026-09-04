package com.manthan.salary_management.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryHistoryResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDate effectiveDate;
    private String reason;
    private LocalDateTime createdAt;
}
