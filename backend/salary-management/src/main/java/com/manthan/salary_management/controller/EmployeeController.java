package com.manthan.salary_management.controller;

import com.manthan.salary_management.dto.request.CreateEmployeeRequest;
import com.manthan.salary_management.dto.request.UpdateEmployeeRequest;
import com.manthan.salary_management.dto.response.EmployeeDetailResponse;
import com.manthan.salary_management.dto.response.EmployeeResponse;
import com.manthan.salary_management.dto.response.SalaryHistoryResponse;
import com.manthan.salary_management.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> getEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(employeeService.getAllEmployees(search, department, country, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDetailResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<EmployeeResponse> deactivateEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.deactivateEmployee(id));
    }

    @GetMapping("/{id}/salary-history")
    public ResponseEntity<List<SalaryHistoryResponse>> getSalaryHistory(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getSalaryHistory(id));
    }
}
