# Salary Management System — Requirements

## Overview
A web-based salary management system for HR teams, built to handle a mid-to-large organization spread across multiple countries. The goal is to replace spreadsheet based salary tracking with a proper system of record that also answers real compensation questions and not just store data, but make it useful.

This is a personal project to practice designing and building a realistic, data-heavy internal tool end-to-end: schema design, query performance at scale, a clean API, and a usable UI, backed by solid tests.

## User Persona
**HR Manager** — manages employee salary data day-to-day and needs to answer compensation questions (pay equity across regions, budget breakdowns, top/bottom earners, etc.) without writing SQL or exporting to Excel.

## Problem Statement
Salary data for a multi-country workforce is commonly managed in spreadsheets: hard to search, easy to break, and impossible to analyze at scale. This project builds a proper application for managing that data and surfacing compensation insights on demand.

## In Scope

### Core data management
- **Employee records:** name, employee ID, department, job title, country, currency, base salary, employment status (active/inactive), date joined.
- **CRUD operations:** add, edit, deactivate, and search/filter employees (by department, country, status, salary range).
- **Salary history:** every salary change is tracked with an effective date, so pay evolution over time is queryable, not just the current snapshot.
- **Seed data:** ~10,000 realistic employee records across multiple countries/departments, for development and testing at real scale.

### Compensation analytics
Rather than a fixed set of hardcoded reports, this is a small **query engine** built on two composable pieces:

- **Dimensions** to slice by: department, country, job title, compensation bracket, employment status.
- **Metrics** to compute per slice: sum, average, median, min, max, count, and share-of-total (%).

Because the underlying query is generic, adding a new insight later means adding a dimension or metric to the engine, not writing a new endpoint.

A handful of common questions sit outside a plain `GROUP BY` and need dedicated logic (ranking, bucketing, or comparing two dimensions at once):

- **Top/bottom N** earners org-wide
- **Compensation brackets** — headcount falling into pay bands like <$50k, $50k–$100k, $100k–$150k, >$150k
- **Company-wide average vs. median** — surfaces skew from high earners in a single comparison
- **Cross-dimension comparisons** — e.g. average pay for a given job title, broken out by country, to compare like-for-like roles across regions

This type of question can be answered out of the box:

| Question | Answered via |
|----------|--------------|
| Total annual payroll across the org | sum, no grouping |
| Which country has the highest payroll cost | sum by country |
| Which department consumes the largest % of budget | share-of-total by department |
| Avg/min/max salary within a department | metrics by department |
| Median salary per country | median by country |
| Headcount-to-payroll ratio by department | count + share-of-total by department |
| Headcount by compensation bracket | bucketing |
| Top 10 highest/lowest-paid employees | ranking |
| Company-wide average vs. median | comparison |
| Avg pay for a job title, across countries | job title × country |
| Salary spread within a job title | metrics by job title |

### Payroll processing
Beyond managing static salary data, the system runs a **monthly payroll process**: for a given month, compute each active employee's final payout (base salary + that month's adjustments) and record it as a permanent snapshot.

- **Salary adjustments:** HR can attach a bonus, deduction, reimbursement, or one-off compensation to a specific employee for an upcoming month, before that month's payroll is run.
- **Payroll run:** produces one line per active employee for the month — base salary, total adjustments, final amount — and persists it as a record, not a live-computed value. Once run, a month's payroll is a fixed snapshot even if the underlying employee/adjustment data changes later.
- **Two trigger paths, one underlying process:**
  - **Manual:** an HR action (hitting an endpoint) runs payroll for a chosen month on demand.
  - **Scheduled:** an automatic monthly run (cron-style) with no manual step required.
  
  Both paths call the same calculation logic — "manual" and "scheduled" differ only in *how* the run is triggered, never in *what* it computes.
- **One run per month:** a payroll run for a given month can only happen once; re-running an already-processed month is rejected rather than silently overwriting the existing snapshot.

This is a payroll *record-and-compute* feature, not a payroll compliance engine — no tax withholding, statutory deductions, or actual money movement. Consistent with the "Payroll processing / tax / statutory compliance" boundary this project generally stays inside.

## Out of Scope

- **Authentication / role-based access** — orthogonal to the core data-and-analytics problem; a natural next addition once the core is solid.
- **Multi-currency conversion** — storing native currency per country is enough to prove out the model; live FX conversion pulls in an external dependency for no added engineering value right now.

## Assumptions
- Single organization, single tenant — no multi-org support needed.
- One currency stored per employee (their local pay currency); no cross-currency rollups for v1.
- "Answer questions about pay" is satisfied via a fixed set of analytical queries rather than free-form natural-language querying.

## Tech Stack
- **Backend:** Java 21 + Spring Boot 3 (Web, Data JPA, Validation), Liquibase
- **Database:** MySQL 8
- **Cache:** Redis
- **Frontend:** Angular
- **Testing:** JUnit 5

## Success Criteria
- Can search/filter across 10,000 employees quickly and accurately.
- Can add/edit salary records and see history of changes over time.
- Can answer compensation questions like the ones above and reasonable variations on them without touching a spreadsheet.
- Runs end-to-end locally (backend + frontend) with seeded data and passing tests.