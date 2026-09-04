package com.manthan.salary_management.repository;

import com.manthan.salary_management.entity.PayrollCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollCycleRepository extends JpaRepository<PayrollCycle, Long> {
    Optional<PayrollCycle> findByMonth(String month);
    boolean existsByMonth(String month);
    List<PayrollCycle> findAllByOrderByMonthDesc();
}
