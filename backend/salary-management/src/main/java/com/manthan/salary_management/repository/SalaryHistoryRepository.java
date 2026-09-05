package com.manthan.salary_management.repository;

import com.manthan.salary_management.dto.projection.EmployeeSalaryProjection;
import com.manthan.salary_management.entity.SalaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryHistoryRepository extends JpaRepository<SalaryHistory, Long> {
    List<SalaryHistory> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
    
    Optional<SalaryHistory> findTopByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
    
    @Query("SELECT sh FROM SalaryHistory sh WHERE sh.employee.id = :employeeId AND sh.effectiveDate = (SELECT MAX(sh2.effectiveDate) FROM SalaryHistory sh2 WHERE sh2.employee.id = :employeeId)")
    Optional<SalaryHistory> findCurrentSalary(@Param("employeeId") Long employeeId);

    @Query(value = "SELECT sh.employee_id AS employeeId, sh.amount AS amount FROM salary_history sh " +
            "INNER JOIN (SELECT employee_id, MAX(effective_date) as max_date FROM salary_history GROUP BY employee_id) latest " +
            "ON sh.employee_id = latest.employee_id AND sh.effective_date = latest.max_date", nativeQuery = true)
    List<EmployeeSalaryProjection> findAllCurrentSalaries();
}

