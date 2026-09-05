package com.manthan.salary_management.service;

import com.manthan.salary_management.dto.response.PaySlipResponse;
import com.manthan.salary_management.dto.response.PayrollCycleResponse;
import com.manthan.salary_management.entity.Employee;
import com.manthan.salary_management.entity.PayrollCycle;
import com.manthan.salary_management.entity.SalaryAdjustment;
import com.manthan.salary_management.entity.PaySlip;
import com.manthan.salary_management.entity.enums.AdjustmentType;
import com.manthan.salary_management.entity.enums.EmploymentStatus;
import com.manthan.salary_management.entity.enums.PayrollStatus;
import com.manthan.salary_management.entity.enums.TriggerType;
import com.manthan.salary_management.exception.DuplicateResourceException;
import com.manthan.salary_management.exception.ResourceNotFoundException;
import com.manthan.salary_management.mapper.PayrollMapper;
import com.manthan.salary_management.repository.EmployeeRepository;
import com.manthan.salary_management.repository.PaySlipRepository;
import com.manthan.salary_management.repository.PayrollCycleRepository;
import com.manthan.salary_management.repository.SalaryAdjustmentRepository;
import com.manthan.salary_management.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollCycleRepository payrollCycleRepository;
    private final PaySlipRepository paySlipRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final SalaryAdjustmentRepository salaryAdjustmentRepository;

    @Transactional
    public PayrollCycleResponse runPayroll(String month, TriggerType triggerType) {
        if (payrollCycleRepository.existsByMonth(month)) {
            throw new DuplicateResourceException("Payroll run already exists for month: " + month);
        }

        log.info("Starting payroll run for month: {}", month);
        long startTime = System.currentTimeMillis();

        PayrollCycle payrollCycle = PayrollCycle.builder()
                .month(month)
                .triggeredBy(triggerType)
                .runAt(LocalDateTime.now())
                .status(PayrollStatus.PROCESSING)
                .build();
        payrollCycle = payrollCycleRepository.save(payrollCycle);

        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == EmploymentStatus.ACTIVE)
                .collect(Collectors.toList());
        log.info("Found {} active employees", activeEmployees.size());

        Map<Long, BigDecimal> salaryMap = new HashMap<>();
        List<Object[]> allSalaries = salaryHistoryRepository.findAllCurrentSalaries();
        for (Object[] row : allSalaries) {
            Long employeeId = ((Number) row[0]).longValue();
            BigDecimal amount = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString());
            salaryMap.put(employeeId, amount);
        }
        log.info("Loaded {} salary records in bulk", salaryMap.size());

        List<SalaryAdjustment> allAdjustments = salaryAdjustmentRepository.findByEffectiveMonth(month);
        Map<Long, List<SalaryAdjustment>> adjustmentMap = allAdjustments.stream()
                .collect(Collectors.groupingBy(adj -> adj.getEmployee().getId()));
        log.info("Loaded {} adjustments for month {}", allAdjustments.size(), month);

        List<PaySlip> paySlips = new ArrayList<>();
        for (Employee employee : activeEmployees) {
            BigDecimal baseSalary = salaryMap.getOrDefault(employee.getId(), BigDecimal.ZERO);

            List<SalaryAdjustment> empAdjustments = adjustmentMap.getOrDefault(employee.getId(), List.of());
            BigDecimal totalAdjustments = BigDecimal.ZERO;
            for (SalaryAdjustment adjustment : empAdjustments) {
                if (adjustment.getType() == AdjustmentType.DEDUCTION) {
                    totalAdjustments = totalAdjustments.subtract(adjustment.getAmount());
                } else {
                    totalAdjustments = totalAdjustments.add(adjustment.getAmount());
                }
            }

            BigDecimal finalAmount = baseSalary.add(totalAdjustments);

            paySlips.add(PaySlip.builder()
                    .payrollCycle(payrollCycle)
                    .employee(employee)
                    .baseSalary(baseSalary)
                    .totalAdjustments(totalAdjustments)
                    .finalAmount(finalAmount)
                    .build());
        }

        paySlipRepository.saveAll(paySlips);

        payrollCycle.setStatus(PayrollStatus.COMPLETED);
        payrollCycle = payrollCycleRepository.save(payrollCycle);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Payroll run completed for month {} — {} employees processed in {}ms", month, paySlips.size(), elapsed);

        return PayrollMapper.toPayrollCycleResponse(payrollCycle, paySlips);
    }

    @Transactional(readOnly = true)
    public PayrollCycleResponse getPayrollCycle(String month) {
        PayrollCycle payrollCycle = payrollCycleRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollCycle", "month", month));
        
        List<PaySlip> paySlips = paySlipRepository.findByPayrollCycleId(payrollCycle.getId());
        
        return PayrollMapper.toPayrollCycleResponse(payrollCycle, paySlips);
    }

    @Transactional(readOnly = true)
    public List<PayrollCycleResponse> getPayrollCycles() {
        return payrollCycleRepository.findAllByOrderByMonthDesc().stream()
                .map(run -> PayrollMapper.toPayrollCycleResponse(run, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PaySlipResponse> getPaySlips(String month, String search, Pageable pageable) {
        PayrollCycle payrollCycle = payrollCycleRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollCycle", "month", month));

        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<PaySlip> page = paySlipRepository.findByPayrollCycleIdWithSearch(
                payrollCycle.getId(), searchParam, pageable);

        return page.map(PayrollMapper::toPaySlipResponse);
    }
}
