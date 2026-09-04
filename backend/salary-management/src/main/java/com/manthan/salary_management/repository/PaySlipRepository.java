package com.manthan.salary_management.repository;

import com.manthan.salary_management.entity.PaySlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaySlipRepository extends JpaRepository<PaySlip, Long> {
    List<PaySlip> findByPayrollCycleId(Long payrollCycleId);
}
