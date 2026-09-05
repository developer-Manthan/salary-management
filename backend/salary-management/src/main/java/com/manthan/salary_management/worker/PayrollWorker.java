package com.manthan.salary_management.worker;

import com.manthan.salary_management.config.RabbitMQConfig;
import com.manthan.salary_management.dto.message.PayrollCycleMessage;
import com.manthan.salary_management.dto.projection.EmployeeSalaryProjection;
import com.manthan.salary_management.entity.Employee;
import com.manthan.salary_management.entity.PayrollCycle;
import com.manthan.salary_management.entity.PaySlip;
import com.manthan.salary_management.entity.SalaryAdjustment;
import com.manthan.salary_management.entity.enums.AdjustmentType;
import com.manthan.salary_management.entity.enums.EmploymentStatus;
import com.manthan.salary_management.entity.enums.PayrollStatus;
import com.manthan.salary_management.repository.EmployeeRepository;
import com.manthan.salary_management.repository.PaySlipRepository;
import com.manthan.salary_management.repository.PayrollCycleRepository;
import com.manthan.salary_management.repository.SalaryAdjustmentRepository;
import com.manthan.salary_management.repository.SalaryHistoryRepository;
import com.manthan.salary_management.service.CacheEvictionService;
import com.manthan.salary_management.service.PayrollPublisher;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollWorker {

    private static final int BATCH_SIZE = 500;
    private static final int MAX_RETRIES = 3;

    private final PayrollCycleRepository payrollCycleRepository;
    private final PaySlipRepository paySlipRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;
    private final SalaryAdjustmentRepository salaryAdjustmentRepository;
    private final PayrollPublisher payrollPublisher;
    private final CacheEvictionService cacheEvictionService;

    @RabbitListener(queues = RabbitMQConfig.PAYROLL_QUEUE, ackMode = "MANUAL")
    public void processPayroll(PayrollCycleMessage message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        Long cycleId = message.getPayrollCycleId();
        int startBatch = message.getStartBatch();

        try {
            // ── Idempotency guard ──
            PayrollCycle cycle = payrollCycleRepository.findById(cycleId).orElseThrow(
                    () -> new IllegalStateException("PayrollCycle not found: " + cycleId));

            if (cycle.getStatus() == PayrollStatus.COMPLETED) {
                log.info("Run {} already COMPLETED, skipping", cycleId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // ── Update status to PROCESSING ──
            cycle.setStatus(PayrollStatus.PROCESSING);
            cycle.setRetryCount(message.getRetryCount());
            cycle.setErrorMessage(null);
            payrollCycleRepository.save(cycle);

            log.info("Run {}: starting from batch {}, retryCount={}", cycleId, startBatch, message.getRetryCount());

            // ── Load data (3 bulk queries) ──
            List<Employee> employees = employeeRepository.findByStatus(EmploymentStatus.ACTIVE);
            employees.sort(Comparator.comparingLong(Employee::getId)); // deterministic order

            Map<Long, BigDecimal> salaryMap = buildSalaryMap();
            Map<Long, List<SalaryAdjustment>> adjustmentMap = buildAdjustmentMap(cycle.getMonth());

            // ── Split into batches ──
            List<List<Employee>> batches = partition(employees, BATCH_SIZE);
            int totalBatches = batches.size();

            log.info("Run {}: {} employees, {} batches", cycleId, employees.size(), totalBatches);

            // ── Process from startBatch onwards ──
            for (int i = startBatch; i < totalBatches; i++) {
                List<PaySlip> batchLines = new ArrayList<>();

                for (Employee emp : batches.get(i)) {
                    BigDecimal baseSalary = salaryMap.getOrDefault(emp.getId(), BigDecimal.ZERO);
                    BigDecimal totalAdj = computeAdjustments(adjustmentMap, emp.getId(), baseSalary);
                    BigDecimal finalAmount = baseSalary.add(totalAdj);

                    batchLines.add(PaySlip.builder()
                            .payrollCycle(cycle)
                            .employee(emp)
                            .baseSalary(baseSalary)
                            .totalAdjustments(totalAdj)
                            .finalAmount(finalAmount)
                            .build());
                }

                // Save batch + update progress atomically
                saveBatchAndUpdateProgress(cycle, batchLines, i);

                log.debug("Run {}: batch {}/{} done ({} employees)", 
                         cycleId, i + 1, totalBatches, cycle.getProcessedCount());
            }

            // ── Mark COMPLETED ──
            cycle.setStatus(PayrollStatus.COMPLETED);
            cycle.setProcessedCount(employees.size());
            payrollCycleRepository.save(cycle);
            cacheEvictionService.evictAllAnalyticsCache();

            long elapsed = System.currentTimeMillis();
            log.info("Run {} COMPLETED: {} employees in {} batches", cycleId, employees.size(), totalBatches);
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            handleFailure(cycleId, message, channel, deliveryTag, e);
        }
    }

    @Transactional
    public void saveBatchAndUpdateProgress(PayrollCycle cycle, List<PaySlip> slips, int batchIndex) {
        paySlipRepository.saveAll(slips);
        cycle.setProcessedCount(cycle.getProcessedCount() + slips.size());
        cycle.setLastCompletedBatch(batchIndex);
        payrollCycleRepository.save(cycle);
    }

    private void handleFailure(Long cycleId, PayrollCycleMessage message, Channel channel, long deliveryTag, Exception e) {
        try {
            PayrollCycle cycle = payrollCycleRepository.findById(cycleId).orElse(null);
            if (cycle == null) {
                log.error("Run {} not found during failure handling", cycleId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            int failedBatch = cycle.getLastCompletedBatch() + 1;

            if (message.getRetryCount() < MAX_RETRIES) {
                // ── Retry: republish with updated startBatch ──
                cycle.setStatus(PayrollStatus.QUEUED);
                cycle.setErrorMessage("Retry " + (message.getRetryCount() + 1)
                        + ": failed at batch " + failedBatch
                        + " — " + truncateMessage(e.getMessage()));
                payrollCycleRepository.save(cycle);

                payrollPublisher.publish(PayrollCycleMessage.builder()
                        .payrollCycleId(cycleId)
                        .month(message.getMonth())
                        .triggerType(message.getTriggerType())
                        .startBatch(failedBatch)
                        .retryCount(message.getRetryCount() + 1)
                        .build());

                log.warn("Run {} failed at batch {}, republished for retry ({}/{})",
                        cycleId, failedBatch, message.getRetryCount() + 1, MAX_RETRIES);
            } else {
                // ── Max retries exceeded ──
                cycle.setStatus(PayrollStatus.FAILED);
                cycle.setErrorMessage("Max retries (" + MAX_RETRIES + ") exceeded at batch "
                        + failedBatch + ": " + truncateMessage(e.getMessage()));
                payrollCycleRepository.save(cycle);

                log.error("Run {} FAILED permanently at batch {}", cycleId, failedBatch, e);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception ackError) {
            log.error("Failed to handle failure for cycle {}", cycleId, ackError);
        }
    }

    private Map<Long, BigDecimal> buildSalaryMap() {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (EmployeeSalaryProjection row : salaryHistoryRepository.findAllCurrentSalaries()) {
            map.put(row.getEmployeeId(), row.getAmount());
        }
        return map;
    }

    private Map<Long, List<SalaryAdjustment>> buildAdjustmentMap(String month) {
        return salaryAdjustmentRepository.findByEffectiveMonth(month).stream()
                .collect(Collectors.groupingBy(adj -> adj.getEmployee().getId()));
    }

    private BigDecimal computeAdjustments(Map<Long, List<SalaryAdjustment>> adjustmentMap,
                                           Long employeeId, BigDecimal baseSalary) {
        List<SalaryAdjustment> adjustments = adjustmentMap.getOrDefault(employeeId, List.of());
        BigDecimal total = BigDecimal.ZERO;
        for (SalaryAdjustment adj : adjustments) {
            if (adj.getType() == AdjustmentType.DEDUCTION) {
                total = total.subtract(adj.getAmount());
            } else {
                total = total.add(adj.getAmount());
            }
        }
        return total;
    }

    private String truncateMessage(String message) {
        if (message == null) return "Unknown error";
        return message.length() > 400 ? message.substring(0, 400) + "..." : message;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
