package com.manthan.salary_management.dto.request;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest {

    private String name;
    private String department;
    private String jobTitle;
    private String country;
    private String currency;

    @Positive(message = "New salary must be positive")
    private BigDecimal newSalary;

    private String salaryChangeReason;
}
