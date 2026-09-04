package com.manthan.salary_management.repository;

import com.manthan.salary_management.entity.SalaryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryAdjustmentRepository extends JpaRepository<SalaryAdjustment, Long> {
    List<SalaryAdjustment> findByEmployeeIdAndEffectiveMonth(Long employeeId, String effectiveMonth);
    List<SalaryAdjustment> findByEmployeeId(Long employeeId);

    List<SalaryAdjustment> findByEffectiveMonth(String effectiveMonth);
}
