package com.manthan.salary_management.service;

import com.manthan.salary_management.dto.request.CreateAdjustmentRequest;
import com.manthan.salary_management.dto.response.AdjustmentResponse;
import com.manthan.salary_management.entity.Employee;
import com.manthan.salary_management.entity.SalaryAdjustment;
import com.manthan.salary_management.entity.enums.AdjustmentType;
import com.manthan.salary_management.exception.ResourceNotFoundException;
import com.manthan.salary_management.mapper.PayrollMapper;
import com.manthan.salary_management.repository.EmployeeRepository;
import com.manthan.salary_management.repository.SalaryAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryAdjustmentService {

    private final SalaryAdjustmentRepository salaryAdjustmentRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public AdjustmentResponse createAdjustment(Long employeeId, CreateAdjustmentRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        AdjustmentType type;
        try {
            type = AdjustmentType.valueOf(request.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid adjustment type: " + request.getType());
        }

        SalaryAdjustment adjustment = SalaryAdjustment.builder()
                .employee(employee)
                .type(type)
                .amount(request.getAmount())
                .effectiveMonth(request.getEffectiveMonth())
                .note(request.getNote())
                .build();

        adjustment = salaryAdjustmentRepository.save(adjustment);
        return PayrollMapper.toAdjustmentResponse(adjustment);
    }

    @Transactional(readOnly = true)
    public List<AdjustmentResponse> getAdjustments(Long employeeId, String month) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", "id", employeeId);
        }

        List<SalaryAdjustment> adjustments;
        if (month != null && !month.trim().isEmpty()) {
            adjustments = salaryAdjustmentRepository.findByEmployeeIdAndEffectiveMonth(employeeId, month);
        } else {
            adjustments = salaryAdjustmentRepository.findByEmployeeId(employeeId);
        }

        return adjustments.stream()
                .map(PayrollMapper::toAdjustmentResponse)
                .collect(Collectors.toList());
    }
}
