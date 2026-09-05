package com.manthan.salary_management.controller;

import com.manthan.salary_management.dto.request.CreateAdjustmentRequest;
import com.manthan.salary_management.dto.response.AdjustmentResponse;
import com.manthan.salary_management.dto.response.PaySlipResponse;
import com.manthan.salary_management.dto.response.PayrollCycleResponse;
import com.manthan.salary_management.entity.enums.TriggerType;
import com.manthan.salary_management.service.PayrollService;
import com.manthan.salary_management.service.SalaryAdjustmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final SalaryAdjustmentService salaryAdjustmentService;

    @PostMapping("/payroll-cycle/{month}")
    public ResponseEntity<PayrollCycleResponse> triggerPayroll(@PathVariable String month) {
        PayrollCycleResponse response = payrollService.runPayroll(month, TriggerType.MANUAL);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/payroll-cycle/{month}")
    public ResponseEntity<PayrollCycleResponse> getPayrollCycle(@PathVariable String month) {
        PayrollCycleResponse response = payrollService.getPayrollCycle(month);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/payroll-cycles")
    public ResponseEntity<List<PayrollCycleResponse>> getPayrollCycles() {
        List<PayrollCycleResponse> responses = payrollService.getPayrollCycles();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pay-slips/{month}")
    public ResponseEntity<Page<PaySlipResponse>> getPaySlips(
            @PathVariable String month,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PaySlipResponse> paySlips = payrollService.getPaySlips(month, search, pageable);
        return ResponseEntity.ok(paySlips);
    }

    @PostMapping("/employees/{employeeId}/adjustments")
    public ResponseEntity<AdjustmentResponse> createAdjustment(
            @PathVariable Long employeeId,
            @Valid @RequestBody CreateAdjustmentRequest request) {
        AdjustmentResponse response = salaryAdjustmentService.createAdjustment(employeeId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/employees/{employeeId}/adjustments")
    public ResponseEntity<List<AdjustmentResponse>> getAdjustments(
            @PathVariable Long employeeId,
            @RequestParam(required = false) String month) {
        List<AdjustmentResponse> responses = salaryAdjustmentService.getAdjustments(employeeId, month);
        return ResponseEntity.ok(responses);
    }
}

