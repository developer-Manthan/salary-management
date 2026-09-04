package com.manthan.salary_management.mapper;

import com.manthan.salary_management.dto.response.AdjustmentResponse;
import com.manthan.salary_management.dto.response.PaySlipResponse;
import com.manthan.salary_management.dto.response.PayrollCycleResponse;
import com.manthan.salary_management.entity.PayrollCycle;
import com.manthan.salary_management.entity.SalaryAdjustment;
import com.manthan.salary_management.entity.PaySlip;

import java.util.List;
import java.util.stream.Collectors;

public class PayrollMapper {

    private PayrollMapper() {
        // Utility class
    }

    public static PayrollCycleResponse toPayrollCycleResponse(PayrollCycle entity, List<PaySlip> paySlips) {
        if (entity == null) {
            return null;
        }

        List<PaySlipResponse> lineResponses = paySlips != null
                ? paySlips.stream().map(PayrollMapper::toPaySlipResponse).collect(Collectors.toList())
                : null;

        return PayrollCycleResponse.builder()
                .id(entity.getId())
                .month(entity.getMonth())
                .triggeredBy(entity.getTriggeredBy().name())
                .runAt(entity.getRunAt())
                .status(entity.getStatus().name())
                .paySlips(lineResponses)
                .build();
    }

    public static PaySlipResponse toPaySlipResponse(PaySlip entity) {
        if (entity == null) {
            return null;
        }

        return PaySlipResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee().getId())
                .employeeCode(entity.getEmployee().getEmployeeCode())
                .employeeName(entity.getEmployee().getName())
                .baseSalary(entity.getBaseSalary())
                .totalAdjustments(entity.getTotalAdjustments())
                .finalAmount(entity.getFinalAmount())
                .build();
    }

    public static AdjustmentResponse toAdjustmentResponse(SalaryAdjustment entity) {
        if (entity == null) {
            return null;
        }

        return AdjustmentResponse.builder()
                .id(entity.getId())
                .type(entity.getType().name())
                .amount(entity.getAmount())
                .effectiveMonth(entity.getEffectiveMonth())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
