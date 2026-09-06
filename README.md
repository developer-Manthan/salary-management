# Salary Management System

A full-stack enterprise payroll and compensation management application designed for HR teams to effortlessly manage 10,000+ employees. Built with an asynchronous, event-driven architecture, strict financial immutability, and proactive caching for real-time analytics.

For deep-dive diagrams, state machines, and design decisions, see the [Architecture Documentation](docs/Architecture.md).

---

## Key Features

- **Strict Financial Immutability**: Salaries are strictly insert-only into `salary_history`; current pay is dynamically derived. Closed payroll runs (`payroll_cycle` and `pay_slip`) are frozen permanent snapshots.
- **Asynchronous Payroll Engine (RabbitMQ)**: Non-blocking execution returning `202 Accepted` in <50ms. Deterministic batching of 500 records per transaction with live polling progress.
- **Batch Retry with Idempotent Resumption**: Failures at any batch roll back only that batch. Automatically retries from `lastCompletedBatch + 1` without duplicate calculations.
- **Real-Time Compensation Analytics**: Multi-dimensional aggregate explorer (Department, Country, Job Title), top-earner rankings, salary bracket distribution, and statistical average-vs-median skew analysis powered by MySQL 8 window functions and typed projections.
- **Proactive Redis Cache Warming**: Analytics queries are cached via `@Cacheable`. Data mutations evict cache keys and immediately trigger a background `@Async` worker to pre-warm the primary dashboard queries before users visit.
- **Employee 360 View**: Multi-attribute filtering (Criteria API), search debounce, salary history progression, pre-payroll adjustments (bonuses/deductions), and individual employee payslips.

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 21, Spring Boot (Web, Data JPA, AMQP, Cache, Validation) |
| **Frontend** | Angular 19 (Standalone Components), Angular Material, ngx-charts |
| **Database** | MySQL with versioned Liquibase YAML migrations |
| **Message Broker** | RabbitMQ (Alpine with Management Plugin) |
| **Cache** | Redis |
| **Reverse Proxy** | Nginx (`salary-management.localhost`) |
| **Containerization** | Docker Compose |

---

## Quick Start

### 1. Start Infrastructure (Docker)
Ensure Docker is running, then start MySQL, Redis, RabbitMQ, and Nginx:
```bash
docker-compose up -d
```

### 2. Start Backend
```bash
cd backend/salary-management
./mvnw spring-boot:run
```
*Note: Liquibase will automatically execute all database migrations on startup.*

### 3. Start Frontend
```bash
cd frontend
npm install
npx ng serve --host 0.0.0.0
```

### 4. Seed Data (10,000 Employees)
To generate 10,000 realistic employee records with salary histories and adjustments, trigger the seed endpoint:
```bash
curl -X POST http://salary-management.localhost/api/v1/seed
```
*(or send a `POST` request to `http://localhost:8080/api/v1/seed` via Postman).*

---

## Service Endpoints & Port Mapping

| Service | URL | Credentials |
|---|---|---|
| **Web Application** | [http://salary-management.localhost](http://salary-management.localhost) *(or `:4200`)* | — |
| **Backend REST API** | `http://localhost:8080/api/v1` | — |
| **RabbitMQ Management** | [http://localhost:15672](http://localhost:15672) | `guest` / `guest` |
| **MySQL Database** | `localhost:3306` (`salary_management`) | `salary_user` / `salary_pass` |
| **Redis Cache** | `localhost:6379` | — |

---

## Documentation

- [Detailed Architecture & Mermaid Diagrams](docs/Architecture.md)
- [Requirements & Specifications](docs/Requirements.md)
