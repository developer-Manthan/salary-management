package com.manthan.salary_management.repository;

import com.manthan.salary_management.entity.Employee;
import com.manthan.salary_management.entity.enums.EmploymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    boolean existsByEmployeeCode(String employeeCode);

    List<Employee> findByStatus(EmploymentStatus status);
    long countByStatus(EmploymentStatus status);

    @Query("SELECT DISTINCT e.jobTitle FROM Employee e ORDER BY e.jobTitle")
    List<String> findDistinctJobTitles();
}
