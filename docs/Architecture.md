# Architecture

## Tech Stack
- **Backend:** Java 21 + Spring Boot (Web, Data JPA, Validation, AMQP), Liquibase
- **Database:** MySQL
- **Cache:** Redis
- **Message Queue:** RabbitMQ
- **Reverse Proxy:** Nginx
- **Frontend:** Angular 19 (standalone components) + Angular Material + ngx-charts

## System Overview

```mermaid
flowchart TB
    subgraph Client
        Browser["HR Manager<br/>(Browser)"]
    end

    subgraph Nginx["Nginx (salary-management.localhost)"]
        Proxy["Reverse Proxy<br/>:80"]
    end

    subgraph Frontend["Frontend (:4200)"]
        Angular["Angular 19<br/>Material UI + ngx-charts"]
    end

    subgraph Backend["Spring Boot API (:8080)"]
        Controllers["REST Controllers<br/>/api/v1/*"]
        Services["Service Layer"]
        Worker["PayrollWorker<br/>@RabbitListener"]
    end

    subgraph Data["Data Layer"]
        MySQL[("MySQL 8<br/>Source of Truth")]
        Redis[("Redis 7<br/>Analytics Cache")]
        RabbitMQ[("RabbitMQ 3<br/>Message Queue")]
    end

    Browser --> Proxy
    Proxy -->|"/* → :4200"| Angular
    Proxy -->|"/api/* → :8080"| Controllers
    Angular -->|"REST / JSON"| Proxy

    Controllers --> Services
    Services -->|"CRUD"| MySQL
    Services -->|"cache read/write"| Redis
    Services -->|"publish"| RabbitMQ
    RabbitMQ -->|"consume"| Worker
    Worker -->|"batch write"| MySQL
    Worker -->|"evict + warm"| Redis
```

## Request Flow — CRUD Operations

```mermaid
sequenceDiagram
    participant UI as Angular
    participant API as Spring Boot
    participant DB as MySQL
    participant Cache as Redis

    UI->>API: POST /api/v1/employees (create)
    API->>DB: INSERT employee
    API->>Cache: @CacheEvict (analytics-*)
    
    rect rgb(240, 248, 255)
        Note over API,Cache: Async Cache Warmup (background thread)
        API-->>Cache: warmAnalyticsCache()
        Cache-->>DB: getSummary("department","avg")
        Cache-->>DB: getTopEarners(10,"desc")
        Cache-->>DB: getBrackets()
        Cache-->>DB: getAvgVsMedian()
        Note over Cache: Redis repopulated
    end
    
    API-->>UI: 201 Created (immediate)
```

## Async Payroll Processing — RabbitMQ

```mermaid
sequenceDiagram
    participant UI as Angular
    participant API as PayrollService
    participant MQ as RabbitMQ
    participant W as PayrollWorker
    participant DB as MySQL

    UI->>API: POST /payroll-cycle/2026-09
    API->>DB: INSERT payroll_cycle (status=QUEUED)
    API->>MQ: publish PayrollCycleMessage<br/>{startBatch: 0, retryCount: 0}
    API-->>UI: 202 Accepted (< 50ms)

    loop Poll every 2 seconds
        UI->>API: GET /payroll-cycle/2026-09
        API->>DB: SELECT status, processedCount
        API-->>UI: {status: PROCESSING, processedCount: 3500}
    end

    MQ->>W: consume message
    W->>DB: UPDATE status → PROCESSING

    loop Process batches of 500
        W->>DB: saveAll(batch lines)
        W->>DB: UPDATE processedCount, lastCompletedBatch
    end

    W->>DB: UPDATE status → COMPLETED
    W->>API: evictAllAnalyticsCache()

    UI->>API: GET /payroll-cycle/2026-09
    API-->>UI: {status: COMPLETED, processedCount: 10000}
    Note over UI: Stop polling, show success
```

## Payroll Batch Retry with Idempotency

```mermaid
sequenceDiagram
    participant MQ as RabbitMQ
    participant W as PayrollWorker
    participant DB as MySQL

    MQ->>W: message {startBatch: 0, retryCount: 0}
    W->>DB: status → PROCESSING

    Note over W,DB: Batch 0-13: ✅ Success (7000 employees)
    W->>DB: lastCompletedBatch = 13

    Note over W: ❌ Batch 14 fails (e.g., DB timeout)
    W->>DB: Rollback batch 14
    W->>DB: status → QUEUED, errorMessage

    W->>MQ: publish NEW message<br/>{startBatch: 14, retryCount: 1}
    W->>MQ: ack original message

    MQ->>W: message {startBatch: 14, retryCount: 1}
    W->>DB: Verify lastCompletedBatch = 13
    W->>DB: status → PROCESSING

    Note over W,DB: Batch 14-19: ✅ Success
    W->>DB: status → COMPLETED
```

## Payroll State Machine

```mermaid
stateDiagram-v2
    [*] --> QUEUED: POST /payroll-cycle/{month}

    QUEUED --> PROCESSING: Worker picks up message
    
    PROCESSING --> COMPLETED: All batches done
    PROCESSING --> QUEUED: Failure (retryCount < 3)<br/>republish with startBatch=N+1
    PROCESSING --> FAILED: Failure (retryCount >= 3)
    
    FAILED --> QUEUED: POST /payroll-cycle/{month}/retry<br/>(manual retry)
    
    COMPLETED --> [*]
```

## Redis Caching Strategy

```mermaid
flowchart TB
    subgraph Writes["Write Path (CRUD)"]
        Create["Employee Create"]
        Update["Employee Update"]
        Deactivate["Employee Deactivate"]
        Payroll["Payroll Completed"]
    end

    subgraph Eviction["Cache Eviction"]
        Evict["@CacheEvict<br/>Clear all analytics-* keys"]
    end

    subgraph Warmup["Async Cache Warmup"]
        direction TB
        W1["getSummary(department, avg)"]
        W2["getTopEarners(10, desc)"]
        W3["getBrackets()"]
        W4["getAvgVsMedian()"]
    end

    subgraph Read["Read Path (Analytics)"]
        Request["GET /analytics/*"]
        Hit{"Cache<br/>Hit?"}
        Redis[("Redis")]
        MySQL[("MySQL")]
    end

    Create --> Evict
    Update --> Evict
    Deactivate --> Evict
    Payroll --> Evict

    Evict -->|"@Async<br/>(background thread)"| Warmup
    W1 --> Redis
    W2 --> Redis
    W3 --> Redis
    W4 --> Redis

    Request --> Hit
    Hit -->|"Yes"| Redis
    Hit -->|"No"| MySQL
    MySQL -->|"@Cacheable<br/>populate cache"| Redis
```

## Data Model

```mermaid
erDiagram
    EMPLOYEE ||--o{ SALARY_HISTORY : has
    EMPLOYEE ||--o{ SALARY_ADJUSTMENT : has
    EMPLOYEE ||--o{ PAY_SLIP : has
    PAYROLL_CYCLE ||--o{ PAY_SLIP : contains
    EMPLOYEE {
        bigint id PK
        string employee_code UK
        string name
        string department
        string job_title
        string country
        string currency
        string status "ACTIVE | INACTIVE"
        date date_joined
    }
    SALARY_HISTORY {
        bigint id PK
        bigint employee_id FK
        decimal amount
        date effective_date
        string reason
    }
    SALARY_ADJUSTMENT {
        bigint id PK
        bigint employee_id FK
        string type "BONUS | DEDUCTION | REIMBURSEMENT | COMPENSATION"
        decimal amount
        string effective_month
        string note
    }
    PAYROLL_CYCLE {
        bigint id PK
        string month UK
        string triggered_by "MANUAL | SCHEDULED"
        datetime run_at
        string status "QUEUED | PROCESSING | COMPLETED | FAILED"
        int total_employees
        int processed_count
        int last_completed_batch
        int retry_count
        string error_message
    }
    PAY_SLIP {
        bigint id PK
        bigint payroll_cycle_id FK
        bigint employee_id FK
        decimal base_salary
        decimal total_adjustments
        decimal final_amount
    }
```

## Backend Layer Architecture

```mermaid
flowchart TB
    subgraph Controllers["Controllers (@RestController)"]
        EC["EmployeeController"]
        PC["PayrollController"]
        AC["AnalyticsController"]
        SC["SeedController"]
    end

    subgraph Services["Services (@Service)"]
        ES["EmployeeService"]
        PS["PayrollService"]
        AS["AnalyticsService"]
        PP["PayrollPublisher"]
        CE["CacheEvictionService"]
        CW["CacheWarmupService"]
    end

    subgraph Worker["Worker (@Component)"]
        PW["PayrollWorker<br/>@RabbitListener"]
    end

    subgraph Repositories["Repositories (JPA)"]
        ER["EmployeeRepository"]
        SR["SalaryHistoryRepository"]
        SAR["SalaryAdjustmentRepository"]
        PRR["PayrollCycleRepository"]
        PRL["PaySlipRepository"]
        AR["AnalyticsRepository<br/>(EntityManager)"]
    end

    subgraph DTOs["DTOs / Projections"]
        Req["Request DTOs"]
        Res["Response DTOs"]
        Proj["Typed Projections"]
        Msg["PayrollCycleMessage"]
    end

    EC --> ES
    PC --> PS
    AC --> AS
    ES --> ER & SR & CE
    PS --> PRR & PRL & PP
    AS --> AR
    CE -->|"@Async"| CW
    CW --> AS
    PP -->|"RabbitMQ"| PW
    PW --> ER & SR & SAR & PRR & PRL & CE
    AR --> Proj
    SR --> Proj
    PRL --> Proj
```

## Infrastructure — Docker Compose

```mermaid
flowchart LR
    subgraph Docker["docker-compose"]
        MySQL["salary-mysql<br/>:3306"]
        Redis["salary-redis<br/>:6379"]
        RabbitMQ["salary-rabbitmq<br/>:5672 / :15672"]
        Nginx["salary-nginx<br/>:80"]
    end

    subgraph Host["Host Machine"]
        Spring["Spring Boot<br/>:8080"]
        Angular["Angular Dev<br/>:4200"]
    end

    Nginx -->|"/* → :4200"| Angular
    Nginx -->|"/api/* → :8080"| Spring
    Spring --> MySQL
    Spring --> Redis
    Spring --> RabbitMQ
```

## Key Design Decisions

**Salary immutability:** Salary changes are INSERT-only into `salary_history`. `Employee` doesn't store a `current_salary` column — it's derived as the most recent `SalaryHistory` row per employee. This makes "how has pay evolved" queryable without a separate audit log.

**Payroll immutability:** A `PayrollCycle` + its `PaySlip` rows are a permanent snapshot. `base_salary` and `total_adjustments` are captured at run time, not recomputed later — so even if an employee's salary changes next month, last month's payroll record stays exactly as it was.

**Async payroll via RabbitMQ:** Payroll runs asynchronously — the API creates a QUEUED record, publishes to RabbitMQ, and returns 202 Accepted in <50ms. The worker processes in batches of 500, updating `processedCount` in DB so the frontend can show real progress via polling. On failure, the message is republished with `startBatch = lastCompletedBatch + 1` (max 3 retries) — no reprocessing of already-completed batches.

**Cache-aside with async warmup:** Analytics queries are cached in Redis via `@Cacheable`. On writes (employee CRUD, payroll completion), all analytics cache entries are evicted via `@CacheEvict`. A background `@Async` thread then pre-warms the 4 most common analytics queries so the next user gets a cache hit instead of a cold miss.

**Typed projections over Object[]:** Native SQL queries return typed projection interfaces (e.g., `EmployeeSalaryProjection`) instead of raw `Object[]` arrays. The `AnalyticsRepository` uses private record classes to map `EntityManager` results to projections, keeping `Object[]` confined to the repository layer.

**Dynamic filtering via Criteria API:** Employee listing supports optional search + department + country + status + jobTitle filters. The Criteria API builds WHERE clauses dynamically based on which params are present — one method handles all 32+ filter combinations.

## API Shape

| Endpoint | Method | Purpose |
|---|---|---|
| `/employees` | GET | Paginated, filterable, sortable employee list |
| `/employees/{id}` | GET | Single employee with salary history |
| `/employees` | POST | Create employee |
| `/employees/{id}` | PUT | Update employee (salary → new SalaryHistory row) |
| `/employees/{id}/deactivate` | PUT | Deactivate employee |
| `/employees/{id}/salary-history` | GET | Salary history for one employee |
| `/employees/job-titles` | GET | Distinct job titles for dropdown |
| `/employees/{id}/adjustments` | POST | Add salary adjustment |
| `/employees/{id}/adjustments` | GET | List adjustments (optional month filter) |
| `/payroll-cycle/{month}` | POST | Trigger async payroll (returns 202) |
| `/payroll-cycle/{month}` | GET | Payroll run status + progress |
| `/payroll-cycle/{month}/retry` | POST | Retry failed payroll from last batch |
| `/pay-slips/{month}` | GET | Paginated payslips with search |
| `/payroll-cycle/{month}/summary` | GET | Aggregate summary (total employees, payout, adjustments) |
| `/payroll-cycle` | GET | All payroll runs (history) |
| `/analytics/summary` | GET | Dimension × metric aggregate |
| `/analytics/top-earners` | GET | Top/bottom N ranking |
| `/analytics/brackets` | GET | Salary bracket distribution |
| `/analytics/avg-vs-median` | GET | Company-wide average vs median |
| `/seed` | POST | Generate 10K test employees |