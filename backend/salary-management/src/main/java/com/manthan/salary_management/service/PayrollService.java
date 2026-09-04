package com.manthan.salary_management.service;

import com.manthan.salary_management.dto.response.PayrollCycleResponse;
import com.manthan.salary_management.entity.Employee;
import com.manthan.salary_management.entity.PayrollCycle;
import com.manthan.salary_management.entity.PaySlip;
import com.manthan.salary_management.entity.enums.EmploymentStatus;
import com.manthan.salary_management.entity.enums.PayrollStatus;
import com.manthan.salary_management.entity.enums.TriggerType;
import com.manthan.salary_management.exception.DuplicateResourceException;
import com.manthan.salary_management.exception.ResourceNotFoundException;
import com.manthan.salary_management.mapper.PayrollMapper;
import com.manthan.salary_management.repository.EmployeeRepository;
import com.manthan.salary_management.repository.PaySlipRepository;
import com.manthan.salary_management.repository.PayrollCycleRepository;
import com.manthan.salary_management.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public PayrollCycleResponse runPayroll(String month, TriggerType triggerType) {
        if (payrollCycleRepository.existsByMonth(month)) {
            throw new DuplicateResourceException("Payroll run already exists for month: " + month);
        }

        log.info("Starting payroll run for month: {}", month);
        long startTime = System.currentTimeMillis();

        // 1. Create payroll run record
        PayrollCycle payrollCycle = PayrollCycle.builder()
                .month(month)
                .triggeredBy(triggerType)
                .runAt(LocalDateTime.now())
                .status(PayrollStatus.PROCESSING)
                .build();
        payrollCycle = payrollCycleRepository.save(payrollCycle);

        // 2. Bulk fetch active employees (1 query)
        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> e.getStatus() == EmploymentStatus.ACTIVE)
                .collect(Collectors.toList());
        log.info("Found {} active employees", activeEmployees.size());

        // 3. Bulk fetch ALL current salaries in one query (1 query instead of N)
        Map<Long, BigDecimal> salaryMap = new HashMap<>();
        List<Object[]> allSalaries = salaryHistoryRepository.findAllCurrentSalaries();
        for (Object[] row : allSalaries) {
            Long employeeId = ((Number) row[0]).longValue();
            BigDecimal amount = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString());
            salaryMap.put(employeeId, amount);
        }
        log.info("Loaded {} salary records in bulk", salaryMap.size());

        // 4. Build paySlips (pure computation, no DB calls)
        List<PaySlip> paySlips = new ArrayList<>();
        for (Employee employee : activeEmployees) {
            BigDecimal baseSalary = salaryMap.getOrDefault(employee.getId(), BigDecimal.ZERO);

            paySlips.add(PaySlip.builder()
                    .payrollCycle(payrollCycle)
                    .employee(employee)
                    .baseSalary(baseSalary)
                    .build());
        }

        // 5. Bulk save all paySlips
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
}
