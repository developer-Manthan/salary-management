package com.manthan.salary_management.seed;

import com.manthan.salary_management.entity.Employee;
import com.manthan.salary_management.entity.SalaryAdjustment;
import com.manthan.salary_management.entity.SalaryHistory;
import com.manthan.salary_management.entity.enums.AdjustmentType;
import com.manthan.salary_management.entity.enums.EmploymentStatus;
import com.manthan.salary_management.repository.EmployeeRepository;
import com.manthan.salary_management.repository.SalaryAdjustmentRepository;
import com.manthan.salary_management.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSeeder {

    private final EmployeeRepository employeeRepository;
    private final SalaryAdjustmentRepository salaryAdjustmentRepository;

    private static final int EMPLOYEES_PER_BATCH = 10000;
    private static final int SAVE_BATCH_SIZE = 500;

    @Transactional
    public SeedResult seed() {
        long existingCount = employeeRepository.count();
        int startIndex = (int) existingCount + 1;

        log.info("Seeding {} employees starting from index {}...", EMPLOYEES_PER_BATCH, startIndex);
        long startTime = System.nanoTime();
        Faker faker = new Faker();

        int totalSalaryHistoryRecords = 0;
        int totalSalaryAdjustments = 0;

        List<Employee> employeeBatch = new ArrayList<>();

        for (int i = startIndex; i < startIndex + EMPLOYEES_PER_BATCH; i++) {
            Employee employee = generateEmployee(faker, i);
            employeeBatch.add(employee);

            if (employeeBatch.size() % SAVE_BATCH_SIZE == 0) {
                List<Employee> savedEmployees = employeeRepository.saveAll(employeeBatch);
                totalSalaryHistoryRecords += savedEmployees.stream()
                        .mapToInt(e -> e.getSalaryHistory().size())
                        .sum();

                List<SalaryAdjustment> adjustments = generateAdjustments(faker, savedEmployees);
                if (!adjustments.isEmpty()) {
                    salaryAdjustmentRepository.saveAll(adjustments);
                    totalSalaryAdjustments += adjustments.size();
                }

                employeeBatch.clear();
                log.info("Progress: {}/{} employees seeded", i - startIndex + 1, EMPLOYEES_PER_BATCH);
            }
        }

        // Save remaining
        if (!employeeBatch.isEmpty()) {
            List<Employee> savedEmployees = employeeRepository.saveAll(employeeBatch);
            totalSalaryHistoryRecords += savedEmployees.stream()
                    .mapToInt(e -> e.getSalaryHistory().size())
                    .sum();
            List<SalaryAdjustment> adjustments = generateAdjustments(faker, savedEmployees);
            if (!adjustments.isEmpty()) {
                salaryAdjustmentRepository.saveAll(adjustments);
                totalSalaryAdjustments += adjustments.size();
            }
        }

        long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
        long newTotal = employeeRepository.count();

        log.info("Seeding complete! {} employees added in {}s. Total now: {}", EMPLOYEES_PER_BATCH, elapsedSeconds, newTotal);

        return new SeedResult(EMPLOYEES_PER_BATCH, totalSalaryHistoryRecords, totalSalaryAdjustments, newTotal, elapsedSeconds);
    }

    public record SeedResult(
            int employeesCreated,
            int salaryHistoryRecords,
            int salaryAdjustments,
            long totalEmployeesInDb,
            long elapsedSeconds
    ) {}

    // ---- Data generation helpers (unchanged) ----

    private Employee generateEmployee(Faker faker, int index) {
        String employeeCode = String.format("EMP-%05d", index);
        String name = faker.name().fullName();

        DepartmentInfo deptInfo = getRandomDepartmentAndTitle(faker);
        CountryInfo countryInfo = getRandomCountry(faker);
        EmploymentStatus status = faker.number().numberBetween(1, 101) <= 92 ? EmploymentStatus.ACTIVE : EmploymentStatus.INACTIVE;

        LocalDate dateJoined = getRandomDate(faker, LocalDate.of(2015, 1, 1), LocalDate.of(2025, 12, 31));

        BigDecimal baseSalary = calculateInitialSalary(faker, countryInfo, deptInfo.title);

        Employee employee = Employee.builder()
                .employeeCode(employeeCode)
                .name(name)
                .department(deptInfo.department)
                .jobTitle(deptInfo.title)
                .country(countryInfo.country)
                .currency("USD")
                .status(status)
                .dateJoined(dateJoined)
                .build();

        List<SalaryHistory> history = generateSalaryHistory(faker, employee, baseSalary, dateJoined);
        employee.setSalaryHistory(history);
        return employee;
    }

    private List<SalaryHistory> generateSalaryHistory(Faker faker, Employee employee, BigDecimal initialSalary, LocalDate dateJoined) {
        List<SalaryHistory> history = new ArrayList<>();
        
        history.add(SalaryHistory.builder()
                .employee(employee)
                .amount(initialSalary)
                .effectiveDate(dateJoined)
                .reason("Initial salary")
                .build());

        int numRaises = faker.number().numberBetween(0, 5);
        LocalDate currentDate = dateJoined;
        BigDecimal currentSalary = initialSalary;
        String[] raiseReasons = {"Annual raise", "Promotion", "Market adjustment", "Performance bonus adjustment", "Role change"};

        for (int i = 0; i < numRaises; i++) {
            int monthsToNextRaise = faker.number().numberBetween(6, 19);
            currentDate = currentDate.plusMonths(monthsToNextRaise);
            
            if (currentDate.isAfter(LocalDate.now())) {
                break;
            }

            double raisePercentage = faker.number().randomDouble(3, 3, 15) / 100.0;
            currentSalary = currentSalary.multiply(BigDecimal.valueOf(1.0 + raisePercentage))
                    .setScale(2, RoundingMode.HALF_UP);

            history.add(SalaryHistory.builder()
                    .employee(employee)
                    .amount(currentSalary)
                    .effectiveDate(currentDate)
                    .reason(raiseReasons[faker.number().numberBetween(0, raiseReasons.length)])
                    .build());
        }

        return history;
    }

    private List<SalaryAdjustment> generateAdjustments(Faker faker, List<Employee> employees) {
        List<SalaryAdjustment> adjustments = new ArrayList<>();
        String[] recentMonths = {"2026-07", "2026-08", "2026-09"};

        for (Employee emp : employees) {
            if (emp.getStatus() == EmploymentStatus.ACTIVE && faker.number().numberBetween(1, 101) <= 20) {
                int numAdjustments = faker.number().numberBetween(1, 4);
                for (int i = 0; i < numAdjustments; i++) {
                    String month = recentMonths[faker.number().numberBetween(0, recentMonths.length)];
                    AdjustmentType type = getRandomAdjustmentType(faker);
                    BigDecimal amount = generateAdjustmentAmount(faker, type);
                    String note = generateAdjustmentNote(type);

                    adjustments.add(SalaryAdjustment.builder()
                            .employee(emp)
                            .type(type)
                            .amount(amount)
                            .effectiveMonth(month)
                            .note(note)
                            .build());
                }
            }
        }
        return adjustments;
    }

    private AdjustmentType getRandomAdjustmentType(Faker faker) {
        int val = faker.number().numberBetween(1, 101);
        if (val <= 40) return AdjustmentType.BONUS;
        if (val <= 60) return AdjustmentType.DEDUCTION;
        if (val <= 85) return AdjustmentType.REIMBURSEMENT;
        return AdjustmentType.COMPENSATION;
    }

    private BigDecimal generateAdjustmentAmount(Faker faker, AdjustmentType type) {
        int amount;
        switch (type) {
            case BONUS: amount = faker.number().numberBetween(500, 10001); break;
            case DEDUCTION: amount = faker.number().numberBetween(100, 3001); break;
            case REIMBURSEMENT: amount = faker.number().numberBetween(200, 5001); break;
            case COMPENSATION: amount = faker.number().numberBetween(1000, 15001); break;
            default: amount = 1000;
        }
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateAdjustmentNote(AdjustmentType type) {
        switch (type) {
            case BONUS: return "Q3 performance bonus";
            case DEDUCTION: return "Insurance deduction";
            case REIMBURSEMENT: return "Travel reimbursement";
            case COMPENSATION: return "Signing bonus";
            default: return "Adjustment";
        }
    }

    private LocalDate getRandomDate(Faker faker, LocalDate start, LocalDate end) {
        long startEpochDay = start.toEpochDay();
        long endEpochDay = end.toEpochDay();
        long randomDay = faker.number().numberBetween(startEpochDay, endEpochDay + 1);
        return LocalDate.ofEpochDay(randomDay);
    }

    private DepartmentInfo getRandomDepartmentAndTitle(Faker faker) {
        int r = faker.number().numberBetween(1, 101);
        String dept;
        String[] titles;

        if (r <= 25) {
            dept = "Engineering";
            titles = new String[]{"Software Engineer", "Senior Software Engineer", "Staff Engineer", "Principal Engineer", "Engineering Manager", "DevOps Engineer", "QA Engineer", "Data Engineer"};
        } else if (r <= 35) {
            dept = "Product";
            titles = new String[]{"Product Manager", "Senior Product Manager", "Product Analyst", "Product Owner"};
        } else if (r <= 43) {
            dept = "Design";
            titles = new String[]{"UI Designer", "UX Designer", "Senior Designer", "Design Lead", "UX Researcher"};
        } else if (r <= 53) {
            dept = "Marketing";
            titles = new String[]{"Marketing Manager", "Content Strategist", "SEO Specialist", "Brand Manager", "Growth Manager"};
        } else if (r <= 68) {
            dept = "Sales";
            titles = new String[]{"Account Executive", "Sales Manager", "Sales Director", "Business Development Rep", "Solutions Engineer"};
        } else if (r <= 75) {
            dept = "HR";
            titles = new String[]{"HR Manager", "HR Business Partner", "Recruiter", "Talent Acquisition Lead", "HR Analyst"};
        } else if (r <= 83) {
            dept = "Finance";
            titles = new String[]{"Financial Analyst", "Senior Accountant", "Finance Manager", "Controller", "FP&A Analyst"};
        } else if (r <= 90) {
            dept = "Operations";
            titles = new String[]{"Operations Manager", "Project Manager", "Program Manager", "Business Analyst", "Operations Analyst"};
        } else if (r <= 95) {
            dept = "Legal";
            titles = new String[]{"Legal Counsel", "Senior Counsel", "Paralegal", "Compliance Officer"};
        } else {
            dept = "Support";
            titles = new String[]{"Support Engineer", "Customer Success Manager", "Support Lead", "Technical Writer"};
        }

        return new DepartmentInfo(dept, titles[faker.number().numberBetween(0, titles.length)]);
    }

    private CountryInfo getRandomCountry(Faker faker) {
        int r = faker.number().numberBetween(1, 101);
        if (r <= 30) return new CountryInfo("US", "USD", 60000, 250000);
        if (r <= 42) return new CountryInfo("UK", "USD", 50000, 220000);
        if (r <= 62) return new CountryInfo("India", "USD", 30000, 150000);
        if (r <= 72) return new CountryInfo("Germany", "USD", 50000, 200000);
        if (r <= 80) return new CountryInfo("Canada", "USD", 50000, 200000);
        if (r <= 87) return new CountryInfo("Australia", "USD", 55000, 210000);
        if (r <= 93) return new CountryInfo("Japan", "USD", 45000, 190000);
        return new CountryInfo("Singapore", "USD", 50000, 200000);
    }

    private BigDecimal calculateInitialSalary(Faker faker, CountryInfo country, String title) {
        int baseRange = country.maxSalary - country.minSalary;
        int randomBase = country.minSalary + faker.number().numberBetween(0, baseRange + 1);
        
        double multiplier;
        String t = title.toLowerCase();
        
        if (t.contains("director") || t.contains("principal") || t.contains("staff") || t.contains("controller") || t.contains("counsel") && !t.contains("senior counsel")) {
            multiplier = faker.number().randomDouble(3, 75, 100) / 100.0;
        } else if (t.contains("senior") || t.contains("lead") || t.contains("senior counsel")) {
            multiplier = faker.number().randomDouble(3, 60, 85) / 100.0;
        } else if (t.contains("junior") || t.contains("rep") || t.contains("analyst") || t.contains("specialist") || t.contains("paralegal") || t.contains("writer")) {
            multiplier = faker.number().randomDouble(3, 30, 50) / 100.0;
        } else {
            multiplier = faker.number().randomDouble(3, 40, 70) / 100.0;
        }

        return BigDecimal.valueOf(randomBase * multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private record DepartmentInfo(String department, String title) {}
    private record CountryInfo(String country, String currency, int minSalary, int maxSalary) {}
}
