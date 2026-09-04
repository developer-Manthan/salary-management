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
public class PaySlipResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private BigDecimal baseSalary;
}
