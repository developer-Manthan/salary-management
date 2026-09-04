package com.manthan.salary_management.mapper;

import com.manthan.salary_management.dto.request.CreateEmployeeRequest;
import com.manthan.salary_management.dto.response.EmployeeDetailResponse;
import com.manthan.salary_management.dto.response.EmployeeResponse;
import com.manthan.salary_management.dto.response.SalaryHistoryResponse;
import com.manthan.salary_management.entity.Employee;
import com.manthan.salary_management.entity.SalaryHistory;
import com.manthan.salary_management.entity.enums.EmploymentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class EmployeeMapper {

    private EmployeeMapper() {
        // Private constructor for utility class
    }

    public static EmployeeResponse toResponse(Employee entity, BigDecimal currentSalary) {
        if (entity == null) {
            return null;
        }

        return EmployeeResponse.builder()
                .id(entity.getId())
                .employeeCode(entity.getEmployeeCode())
                .name(entity.getName())
                .department(entity.getDepartment())
                .jobTitle(entity.getJobTitle())
                .country(entity.getCountry())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .dateJoined(entity.getDateJoined())
                .currentSalary(currentSalary)
                .build();
    }

    public static EmployeeDetailResponse toDetailResponse(Employee entity, BigDecimal currentSalary, List<SalaryHistory> history) {
        if (entity == null) {
            return null;
        }

        List<SalaryHistoryResponse> historyResponses = history != null
                ? history.stream().map(EmployeeMapper::toSalaryHistoryResponse).collect(Collectors.toList())
                : null;

        return EmployeeDetailResponse.builder()
                .id(entity.getId())
                .employeeCode(entity.getEmployeeCode())
                .name(entity.getName())
                .department(entity.getDepartment())
                .jobTitle(entity.getJobTitle())
                .country(entity.getCountry())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .dateJoined(entity.getDateJoined())
                .currentSalary(currentSalary)
                .salaryHistory(historyResponses)
                .build();
    }

    public static SalaryHistoryResponse toSalaryHistoryResponse(SalaryHistory entity) {
        if (entity == null) {
            return null;
        }

        return SalaryHistoryResponse.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .effectiveDate(entity.getEffectiveDate())
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static Employee toEntity(CreateEmployeeRequest request) {
        if (request == null) {
            return null;
        }

        String employeeCode = request.getEmployeeCode();
        if (employeeCode == null || employeeCode.trim().isEmpty()) {
            employeeCode = generateEmployeeCode();
        }

        return Employee.builder()
                .employeeCode(employeeCode)
                .name(request.getName())
                .department(request.getDepartment())
                .jobTitle(request.getJobTitle())
                .country(request.getCountry())
                .currency(request.getCurrency())
                .status(EmploymentStatus.ACTIVE)
                .dateJoined(request.getDateJoined())
                .build();
    }
    
    private static String generateEmployeeCode() {
        Random random = new Random();
        int randomNum = 10000 + random.nextInt(90000);
        return "EMP-" + randomNum;
    }
}
