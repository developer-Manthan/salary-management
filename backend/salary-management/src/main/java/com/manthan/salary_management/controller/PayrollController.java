package com.manthan.salary_management.controller;

import com.manthan.salary_management.dto.response.PayrollCycleResponse;
import com.manthan.salary_management.entity.enums.TriggerType;
import com.manthan.salary_management.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

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
}

