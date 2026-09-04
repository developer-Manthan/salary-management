package com.manthan.salary_management.dto.response;

import com.manthan.salary_management.entity.enums.EmploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String employeeCode;
    private String name;
    private String department;
    private String jobTitle;
    private String country;
    private String currency;
    private EmploymentStatus status;
    private LocalDate dateJoined;
    private BigDecimal currentSalary;
}
