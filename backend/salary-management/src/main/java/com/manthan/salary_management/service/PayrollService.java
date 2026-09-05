package com.manthan.salary_management.service;

import com.manthan.salary_management.dto.message.PayrollCycleMessage;
import com.manthan.salary_management.dto.projection.PayrollSummaryProjection;
import com.manthan.salary_management.dto.response.PaySlipResponse;
import com.manthan.salary_management.dto.response.PayrollCycleResponse;
import com.manthan.salary_management.dto.response.PayrollCycleSummaryResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollCycleRepository payrollCycleRepository;
    private final PaySlipRepository paySlipRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollPublisher payrollPublisher;

    @Transactional
    public PayrollCycleResponse runPayroll(String month, TriggerType triggerType) {
        if (payrollCycleRepository.existsByMonth(month)) {
            throw new DuplicateResourceException("Payroll run already exists for month: " + month);
        }

        long activeCount = employeeRepository.countByStatus(EmploymentStatus.ACTIVE);

        PayrollCycle payrollCycle = PayrollCycle.builder()
                .month(month)
                .triggeredBy(triggerType)
                .runAt(LocalDateTime.now())
                .status(PayrollStatus.QUEUED)
                .totalEmployees((int) activeCount)
                .processedCount(0)
                .lastCompletedBatch(-1)
                .retryCount(0)
                .build();
        payrollCycle = payrollCycleRepository.save(payrollCycle);

        payrollPublisher.publish(PayrollCycleMessage.builder()
                .payrollCycleId(payrollCycle.getId())
                .month(month)
                .triggerType(triggerType.name())
                .startBatch(0)
                .retryCount(0)
                .build());

        log.info("Payroll run QUEUED for month {} — {} active employees", month, activeCount);

        return PayrollMapper.toPayrollCycleResponse(payrollCycle, null);
    }

    @Transactional
    public PayrollCycleResponse retryPayroll(String month) {
        PayrollCycle cycle = payrollCycleRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollCycle", "month", month));

        if (cycle.getStatus() != PayrollStatus.FAILED) {
            throw new IllegalStateException("Can only retry FAILED payroll runs. Current status: " + cycle.getStatus());
        }

        int resumeBatch = cycle.getLastCompletedBatch() + 1;

        payrollPublisher.publish(PayrollCycleMessage.builder()
                .payrollCycleId(cycle.getId())
                .month(month)
                .triggerType(cycle.getTriggeredBy().name())
                .startBatch(resumeBatch)
                .retryCount(cycle.getRetryCount() + 1)
                .build());

        cycle.setStatus(PayrollStatus.QUEUED);
        cycle.setErrorMessage(null);
        payrollCycleRepository.save(cycle);

        log.info("Payroll retry QUEUED for month {} — resuming from batch {}", month, resumeBatch);

        return PayrollMapper.toPayrollCycleResponse(cycle, null);
    }

    @Transactional(readOnly = true)
    public PayrollCycleResponse getPayrollCycle(String month) {
        PayrollCycle payrollCycle = payrollCycleRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollCycle", "month", month));
        
        List<PaySlip> paySlips = payrollCycle.getStatus() == PayrollStatus.COMPLETED
                ? paySlipRepository.findByPayrollCycleId(payrollCycle.getId())
                : null;
        
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

    @Transactional(readOnly = true)
    public PayrollCycleSummaryResponse getPayrollCycleSummary(String month) {
        PayrollCycle payrollCycle = payrollCycleRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollCycle", "month", month));

        PayrollSummaryProjection summary = paySlipRepository.getSummaryByPayrollCycleId(payrollCycle.getId());

        return PayrollCycleSummaryResponse.builder()
                .totalEmployees(summary.getTotalEmployees())
                .totalPayout(summary.getTotalPayout())
                .totalAdjustments(summary.getTotalAdjustments())
                .build();
    }
}
