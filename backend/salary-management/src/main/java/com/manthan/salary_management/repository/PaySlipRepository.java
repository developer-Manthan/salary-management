package com.manthan.salary_management.repository;

import com.manthan.salary_management.dto.projection.PayrollSummaryProjection;
import com.manthan.salary_management.entity.PaySlip;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaySlipRepository extends JpaRepository<PaySlip, Long> {
    List<PaySlip> findByPayrollCycleId(Long payrollCycleId);

    @Query("SELECT ps FROM PaySlip ps JOIN FETCH ps.employee e " +
           "WHERE ps.payrollCycle.id = :payrollCycleId " +
           "AND (:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<PaySlip> findByPayrollCycleIdWithSearch(
            @Param("payrollCycleId") Long payrollCycleId,
            @Param("search") String search,
            Pageable pageable);
       
    @Query("SELECT COUNT(ps) AS totalEmployees, COALESCE(SUM(ps.finalAmount), 0) AS totalPayout, COALESCE(SUM(ps.totalAdjustments), 0) AS totalAdjustments " +
           "FROM PaySlip ps WHERE ps.payrollCycle.id = :payrollCycleId")
    PayrollSummaryProjection getSummaryByPayrollCycleId(@Param("payrollCycleId") Long payrollCycleId);
}
