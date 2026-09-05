package com.manthan.salary_management.dto.projection;

import java.math.BigDecimal;

public interface PayrollSummaryProjection {
    Long getTotalEmployees();
    BigDecimal getTotalPayout();
    BigDecimal getTotalAdjustments();
}
