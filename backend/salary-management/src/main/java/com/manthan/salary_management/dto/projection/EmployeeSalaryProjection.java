package com.manthan.salary_management.dto.projection;

import java.math.BigDecimal;

public interface EmployeeSalaryProjection {
    Long getEmployeeId();
    BigDecimal getAmount();
}
