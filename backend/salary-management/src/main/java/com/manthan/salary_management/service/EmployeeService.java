package com.manthan.salary_management.service;

import com.manthan.salary_management.dto.request.CreateEmployeeRequest;
import com.manthan.salary_management.dto.request.UpdateEmployeeRequest;
import com.manthan.salary_management.dto.response.EmployeeDetailResponse;
import com.manthan.salary_management.dto.response.EmployeeResponse;
import com.manthan.salary_management.dto.response.SalaryHistoryResponse;
import com.manthan.salary_management.entity.Employee;
import com.manthan.salary_management.entity.SalaryHistory;
import com.manthan.salary_management.entity.enums.EmploymentStatus;
import com.manthan.salary_management.exception.DuplicateResourceException;
import com.manthan.salary_management.exception.ResourceNotFoundException;
import com.manthan.salary_management.mapper.EmployeeMapper;
import com.manthan.salary_management.repository.EmployeeRepository;
import com.manthan.salary_management.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SalaryHistoryRepository salaryHistoryRepository;

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(String search, String department, String country, String status, Pageable pageable) {
        Specification<Employee> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate codeLike = cb.like(cb.lower(root.get("employeeCode")), pattern);
                Predicate titleLike = cb.like(cb.lower(root.get("jobTitle")), pattern);
                predicates.add(cb.or(nameLike, codeLike, titleLike));
            }
            if (StringUtils.hasText(department)) {
                predicates.add(cb.equal(root.get("department"), department));
            }
            if (StringUtils.hasText(country)) {
                predicates.add(cb.equal(root.get("country"), country));
            }
            if (StringUtils.hasText(status)) {
                try {
                    EmploymentStatus empStatus = EmploymentStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), empStatus));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status format for filtering, or handle accordingly
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return employeeRepository.findAll(spec, pageable).map(employee -> {
            BigDecimal currentSalary = salaryHistoryRepository.findTopByEmployeeIdOrderByEffectiveDateDesc(employee.getId())
                    .map(SalaryHistory::getAmount)
                    .orElse(BigDecimal.ZERO);
            return EmployeeMapper.toResponse(employee, currentSalary);
        });
    }

    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeById(Long id) {
        Employee employee = findEmployeeOrThrow(id);
        List<SalaryHistory> history = salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(id);
        BigDecimal currentSalary = history.isEmpty() ? BigDecimal.ZERO : history.get(0).getAmount();
        return EmployeeMapper.toDetailResponse(employee, currentSalary, history);
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        String employeeCode = request.getEmployeeCode();
        if (StringUtils.hasText(employeeCode)) {
            if (employeeRepository.existsByEmployeeCode(employeeCode)) {
                throw new DuplicateResourceException("Employee code already exists: " + employeeCode);
            }
        } else {
            Random random = new Random();
            do {
                employeeCode = "EMP-" + (10000 + random.nextInt(90000));
            } while (employeeRepository.existsByEmployeeCode(employeeCode));
            request.setEmployeeCode(employeeCode);
        }

        Employee employee = EmployeeMapper.toEntity(request);
        employee.setStatus(EmploymentStatus.ACTIVE);
        employee = employeeRepository.save(employee);

        SalaryHistory initialSalary = SalaryHistory.builder()
                .employee(employee)
                .amount(request.getInitialSalary())
                .effectiveDate(request.getDateJoined())
                .reason("Initial salary")
                .build();
        salaryHistoryRepository.save(initialSalary);

        return EmployeeMapper.toResponse(employee, initialSalary.getAmount());
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = findEmployeeOrThrow(id);

        if (StringUtils.hasText(request.getName())) {
            employee.setName(request.getName());
        }
        if (StringUtils.hasText(request.getDepartment())) {
            employee.setDepartment(request.getDepartment());
        }
        if (StringUtils.hasText(request.getJobTitle())) {
            employee.setJobTitle(request.getJobTitle());
        }
        if (StringUtils.hasText(request.getCountry())) {
            employee.setCountry(request.getCountry());
        }
        if (StringUtils.hasText(request.getCurrency())) {
            employee.setCurrency(request.getCurrency());
        }

        BigDecimal currentSalaryAmount = salaryHistoryRepository.findTopByEmployeeIdOrderByEffectiveDateDesc(employee.getId())
                .map(SalaryHistory::getAmount)
                .orElse(BigDecimal.ZERO);

        if (request.getNewSalary() != null) {
            SalaryHistory newSalary = SalaryHistory.builder()
                    .employee(employee)
                    .amount(request.getNewSalary())
                    .effectiveDate(LocalDate.now())
                    .reason(request.getSalaryChangeReason())
                    .build();
            salaryHistoryRepository.save(newSalary);
            currentSalaryAmount = newSalary.getAmount();
        }

        employeeRepository.save(employee);
                
        return EmployeeMapper.toResponse(employee, currentSalaryAmount);
    }

    @Transactional
    public EmployeeResponse deactivateEmployee(Long id) {
        Employee employee = findEmployeeOrThrow(id);
        employee.setStatus(EmploymentStatus.INACTIVE);
        employeeRepository.save(employee);
        
        BigDecimal currentSalary = salaryHistoryRepository.findTopByEmployeeIdOrderByEffectiveDateDesc(employee.getId())
                .map(SalaryHistory::getAmount)
                .orElse(BigDecimal.ZERO);
                                
        return EmployeeMapper.toResponse(employee, currentSalary);
    }

    @Transactional(readOnly = true)
    public List<SalaryHistoryResponse> getSalaryHistory(Long employeeId) {
        findEmployeeOrThrow(employeeId);
        List<SalaryHistory> history = salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId);
        return history.stream()
                .map(EmployeeMapper::toSalaryHistoryResponse)
                .collect(Collectors.toList());
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }
}
