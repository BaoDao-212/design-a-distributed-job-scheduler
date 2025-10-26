# Distributed Job Scheduler - Diagrams

This document contains various diagrams that visualize the system architecture, flows, and interactions.

## 1. Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        Client[Client Applications]
    end
    
    subgraph "Service Layer"
        JS[Job Store Service<br/>Port 8081<br/>Persistence Layer]
        SC[Scheduler Coordinator<br/>Port 8082<br/>Leader Election & Segments]
        SW[Scheduler Worker<br/>Port 8083<br/>Job Scanning]
        WA[Worker Agent<br/>Port 8084<br/>Job Execution]
        EC[Execution Coordinator<br/>Port 8085<br/>Health Monitoring]
    end
    
    subgraph "Data Layer"
        DB[(H2/PostgreSQL<br/>Database)]
    end
    
    Client -->|Submit Jobs| JS
    Client -->|Query Status| JS
    
    SW -->|Poll Due Jobs| JS
    SW -->|Update Status| JS
    WA -->|Update Execution| JS
    
    SC -->|Register & Heartbeat| SW
    SC -->|Assign Segments| SW
    
    SW -->|Dispatch Jobs| WA
    
    EC -->|Monitor Health| WA
    EC -->|Reassign Failed Jobs| WA
    
    JS -.->|Read/Write| DB
    SC -.->|Read/Write| DB
    SW -.->|Read/Write| DB
    EC -.->|Read/Write| DB
    
    style JS fill:#e1f5ff
    style SC fill:#fff4e1
    style SW fill:#e8f5e9
    style WA fill:#f3e5f5
    style EC fill:#fce4ec
```

## 2. Job Submission Flow

```mermaid
sequenceDiagram
    actor Client
    participant JS as Job Store Service
    participant DB as Database
    participant SC as Scheduler Coordinator
    participant SW as Scheduler Worker
    
    Client->>JS: POST /api/jobs<br/>(Job Details)
    activate JS
    
    JS->>DB: Save JobEntity<br/>(status=PENDING)
    DB-->>JS: Job Saved (id=123)
    
    JS->>DB: Save JobScheduleEntity<br/>(next_run_time, segment)
    DB-->>JS: Schedule Saved
    
    JS-->>Client: JobResponse<br/>(id=123, status=PENDING)
    deactivate JS
    
    Note over SC,SW: Background Process
    SW->>SC: Register Worker
    SC->>SC: Assign Segments (0-9)
    SC-->>SW: Segments Assigned
```

## 3. Job Scheduling and Execution Flow

```mermaid
sequenceDiagram
    participant SW as Scheduler Worker
    participant JS as Job Store Service
    participant WA as Worker Agent
    participant EC as Execution Coordinator
    
    Note over SW: Every 60 seconds
    
    SW->>JS: GET /api/job-schedules/due<br/>?asOf=now&segments=0-9
    activate JS
    JS->>JS: Query jobs WHERE<br/>next_run_time <= now<br/>AND segment IN (0-9)
    JS-->>SW: List of Due Jobs
    deactivate JS
    
    loop For each due job
        SW->>SW: Create Dispatch Record<br/>(idempotency)
        SW->>JS: PUT /api/jobs/{id}/status<br/>?status=SCHEDULED
        SW->>WA: POST /api/worker-agent/dispatch<br/>(JobDispatchEvent)
        
        activate WA
        WA->>WA: Acquire Semaphore<br/>(Concurrency Control)
        WA->>JS: PUT /api/jobs/{id}/status<br/>?status=RUNNING
        
        WA->>WA: Execute Job Logic<br/>(Simulate Work)
        
        alt Success
            WA->>JS: PUT /api/jobs/{id}/status<br/>?status=COMPLETED
        else Failure
            WA->>JS: PUT /api/jobs/{id}/status<br/>?status=FAILED
        end
        
        WA->>WA: Release Semaphore
        deactivate WA
    end
    
    Note over EC: Every 30 seconds
    EC->>EC: Monitor Worker<br/>Heartbeats
```

## 4. Component Dependencies

```mermaid
graph LR
    subgraph "common"
        Enums[Enums<br/>JobStatus, WorkerStatus]
        DTOs[DTOs<br/>JobDispatchEvent, ScheduledJobResponse]
        Response[Response Utils<br/>ResponseData, ResponseUtils]
    end
    
    subgraph "job-store-service"
        JSController[Controllers]
        JSService[Services]
        JSRepo[Repositories]
        JSEntity[Entities]
    end
    
    subgraph "scheduler-coordinator"
        SCController[Controllers]
        SCService[Services<br/>Leader Election]
        SCRepo[Repositories]
        SCEntity[Entities]
    end
    
    subgraph "scheduler-worker"
        SWController[Controllers]
        SWService[Scheduler Service]
        SWConfig[Worker Config]
    end
    
    subgraph "worker-agent"
        WAController[Controllers]
        WAService[Execution Service]
    end
    
    subgraph "execution-coordinator"
        ECController[Controllers]
        ECService[Monitoring Service]
        ECRepo[Repositories]
    end
    
    Enums -.-> JSEntity
    Enums -.-> SCEntity
    DTOs -.-> SWService
    DTOs -.-> WAService
    Response -.-> JSController
    Response -.-> SCController
    Response -.-> SWController
    Response -.-> WAController
    Response -.-> ECController
    
    style common fill:#e3f2fd
    style job-store-service fill:#e1f5ff
    style scheduler-coordinator fill:#fff4e1
    style scheduler-worker fill:#e8f5e9
    style worker-agent fill:#f3e5f5
    style execution-coordinator fill:#fce4ec
```

## 5. Segment-Based Partitioning

```mermaid
graph TB
    subgraph "100 Segments (0-99)"
        S0_9[Segments 0-9]
        S10_19[Segments 10-19]
        S20_29[Segments 20-29]
        S30_99[Segments 30-99...]
    end
    
    subgraph "Scheduler Workers"
        SW1[Scheduler Worker 1<br/>Segments: 0-9]
        SW2[Scheduler Worker 2<br/>Segments: 10-19]
        SW3[Scheduler Worker 3<br/>Segments: 20-29]
        SWN[Scheduler Worker N<br/>Segments: 30-39]
    end
    
    subgraph "Jobs Distribution"
        J1[Job 1<br/>segment=5]
        J2[Job 2<br/>segment=15]
        J3[Job 3<br/>segment=25]
        J4[Job 4<br/>segment=35]
    end
    
    S0_9 -.->|Assigned to| SW1
    S10_19 -.->|Assigned to| SW2
    S20_29 -.->|Assigned to| SW3
    S30_99 -.->|Assigned to| SWN
    
    J1 -->|Processed by| SW1
    J2 -->|Processed by| SW2
    J3 -->|Processed by| SW3
    J4 -->|Processed by| SWN
    
    style SW1 fill:#a5d6a7
    style SW2 fill:#90caf9
    style SW3 fill:#ffcc80
    style SWN fill:#ce93d8
```

## 6. Leader Election Process

```mermaid
sequenceDiagram
    participant C1 as Coordinator 1
    participant C2 as Coordinator 2
    participant C3 as Coordinator 3
    participant DB as Database
    
    Note over C1,C3: Initial Startup
    
    C1->>DB: Register Node<br/>(priority=1)
    DB->>DB: No leader exists
    DB-->>C1: Elected as LEADER
    
    C2->>DB: Register Node<br/>(priority=2)
    DB->>DB: Leader exists
    DB-->>C2: Registered as FOLLOWER
    
    C3->>DB: Register Node<br/>(priority=3)
    DB->>DB: Leader exists
    DB-->>C3: Registered as FOLLOWER
    
    Note over C1,C3: Normal Operation
    
    loop Every 30 seconds
        C1->>DB: Heartbeat
        C2->>DB: Heartbeat
        C3->>DB: Heartbeat
    end
    
    Note over C1: Leader Failure
    C1->>C1: ❌ Crashes
    
    Note over C2,C3: Failure Detection
    C2->>DB: Check Leader Heartbeat
    DB-->>C2: No heartbeat for 60s
    
    C2->>DB: Elect New Leader<br/>(highest priority follower)
    DB-->>C2: Elected as LEADER
    
    Note over C2,C3: New Leader Active
    C2->>C2: Take over<br/>coordination duties
```

## 7. Worker Health Monitoring

```mermaid
sequenceDiagram
    participant WA as Worker Agent
    participant EC as Execution Coordinator
    participant DB as Database
    participant WA2 as Worker Agent 2
    
    Note over WA,EC: Normal Operation
    
    loop Every 10 seconds
        WA->>EC: POST /workers/{id}/heartbeat<br/>(currentLoad=5)
        EC->>DB: Update last_heartbeat<br/>& current_load
    end
    
    Note over WA: Worker Failure
    WA->>WA: ❌ Crashes
    
    Note over EC: Monitoring (Every 30s)
    EC->>DB: Get all ONLINE workers
    DB-->>EC: Worker List
    
    EC->>EC: Check each worker's<br/>last_heartbeat
    EC->>EC: Worker A: 65 seconds ago<br/>❌ UNHEALTHY
    
    EC->>DB: Update status=UNHEALTHY<br/>for Worker A
    
    EC->>DB: Get jobs assigned<br/>to Worker A
    DB-->>EC: List of jobs<br/>(job_id: 123, 456)
    
    loop For each job
        EC->>EC: Log reassignment
        EC->>WA2: POST /dispatch<br/>(Reassign job)
        EC->>DB: Update execution_assignment<br/>(new worker_id)
    end
    
    Note over EC,WA2: Jobs reassigned successfully
```

## 8. Database Schema Relationships

```mermaid
erDiagram
    JOBS ||--o{ JOB_SCHEDULES : has
    JOBS ||--o{ JOB_EXECUTIONS : has
    JOBS {
        bigint id PK
        varchar job_name
        bigint user_id
        varchar frequency
        text payload
        timestamp execution_time
        int retry_count
        int max_retries
        varchar status
        timestamp created_at
        timestamp updated_at
    }
    
    JOB_SCHEDULES {
        bigint id PK
        bigint job_id FK
        timestamp next_run_time
        timestamp last_run_time
        int segment
    }
    
    JOB_EXECUTIONS {
        bigint id PK
        bigint job_id FK
        varchar worker_id
        timestamp start_time
        timestamp end_time
        varchar status
        text error_message
        text checkpoint_data
        timestamp created_at
    }
    
    WORKERS {
        bigint id PK
        varchar worker_id UK
        varchar status
        int segment
        int capacity
        int current_load
        timestamp created_at
        timestamp updated_at
    }
    
    WORKER_HEARTBEATS {
        bigint id PK
        varchar worker_id UK
        varchar ip_address
        timestamp last_heartbeat
        int available_capacity
    }
    
    COORDINATOR_NODES {
        bigint id PK
        varchar node_id UK
        boolean leader
        int priority
        timestamp last_heartbeat
        timestamp created_at
        timestamp updated_at
    }
    
    SEGMENT_ASSIGNMENTS {
        bigint id PK
        varchar worker_id
        int segment
        timestamp assigned_at
    }
    
    SCHEDULER_WORKERS {
        bigint id PK
        varchar worker_id UK
        varchar status
        varchar assigned_segments
        timestamp last_heartbeat
        timestamp created_at
        timestamp updated_at
    }
    
    EXECUTION_WORKERS {
        bigint id PK
        varchar worker_id UK
        varchar status
        int capacity
        int current_load
        timestamp last_heartbeat
        timestamp created_at
        timestamp updated_at
    }
    
    EXECUTION_ASSIGNMENTS {
        bigint id PK
        bigint job_id
        varchar worker_id
        varchar status
        text checkpoint_data
        timestamp created_at
        timestamp updated_at
    }
```

## 9. Concurrency Control Flow

```mermaid
graph TB
    Start([Job Dispatch Event Received])
    TrySemaphore{Try Acquire<br/>Semaphore}
    Rejected[Log: Concurrency<br/>Limit Reached]
    UpdateRunning[Update Status<br/>to RUNNING]
    Execute[Execute Job Logic]
    Success{Job<br/>Success?}
    UpdateCompleted[Update Status<br/>to COMPLETED]
    UpdateFailed[Update Status<br/>to FAILED]
    ReleaseSemaphore[Release Semaphore]
    End([End])
    
    Start --> TrySemaphore
    TrySemaphore -->|No Permit| Rejected
    Rejected --> End
    TrySemaphore -->|Permit Acquired| UpdateRunning
    UpdateRunning --> Execute
    Execute --> Success
    Success -->|Yes| UpdateCompleted
    Success -->|No| UpdateFailed
    UpdateCompleted --> ReleaseSemaphore
    UpdateFailed --> ReleaseSemaphore
    ReleaseSemaphore --> End
    
    style Start fill:#90caf9
    style TrySemaphore fill:#fff59d
    style Rejected fill:#ef9a9a
    style UpdateCompleted fill:#a5d6a7
    style UpdateFailed fill:#ef9a9a
    style End fill:#90caf9
```

## 10. Idempotency Pattern

```mermaid
graph TB
    Start([Scheduler Worker:<br/>Job Due for Execution])
    CheckDispatch{Check Dispatch<br/>Record Exists?}
    CreateRecord[Create Dispatch<br/>Record in DB]
    DispatchJob[Dispatch Job to<br/>Worker Agent]
    UpdateStatus[Update Job Status<br/>to SCHEDULED]
    Skip[Skip:<br/>Already Dispatched]
    End([End])
    
    Start --> CheckDispatch
    CheckDispatch -->|No| CreateRecord
    CheckDispatch -->|Yes| Skip
    CreateRecord --> DispatchJob
    DispatchJob --> UpdateStatus
    UpdateStatus --> End
    Skip --> End
    
    style Start fill:#90caf9
    style CheckDispatch fill:#fff59d
    style CreateRecord fill:#a5d6a7
    style Skip fill:#ffcc80
    style End fill:#90caf9
```

## 11. Failure and Retry Flow

```mermaid
stateDiagram-v2
    [*] --> PENDING: Job Submitted
    PENDING --> SCHEDULED: Scheduler Worker<br/>Dispatches Job
    SCHEDULED --> RUNNING: Worker Agent<br/>Starts Execution
    
    RUNNING --> COMPLETED: Success
    RUNNING --> FAILED: Error Occurred
    
    FAILED --> PENDING: retry_count < max_retries<br/>Retry with Backoff
    FAILED --> FAILED: retry_count >= max_retries<br/>Permanent Failure
    
    PENDING --> CANCELLED: User Cancels
    SCHEDULED --> CANCELLED: User Cancels
    
    COMPLETED --> [*]
    CANCELLED --> [*]
    FAILED --> [*]: Max Retries Reached
    
    note right of RUNNING
        Worker Agent
        - Acquire Semaphore
        - Execute Job
        - Update Status
        - Release Semaphore
    end note
    
    note right of FAILED
        Retry Logic
        - Increment retry_count
        - Exponential Backoff
        - 1min, 5min, 10min...
    end note
```

## 12. Horizontal Scaling Strategy

```mermaid
graph TB
    subgraph "Time T0: Initial Setup"
        SW1_T0[Scheduler Worker 1<br/>Segments: 0-49]
        SW2_T0[Scheduler Worker 2<br/>Segments: 50-99]
    end
    
    subgraph "Time T1: Scale Up - Add Worker"
        SW1_T1[Scheduler Worker 1<br/>Segments: 0-32]
        SW2_T1[Scheduler Worker 2<br/>Segments: 33-65]
        SW3_T1[Scheduler Worker 3<br/>Segments: 66-99]
    end
    
    subgraph "Time T2: Scale Down - Remove Worker"
        SW1_T2[Scheduler Worker 1<br/>Segments: 0-49]
        SW2_T2[Scheduler Worker 2<br/>Segments: 50-99]
    end
    
    Rebalance1[Coordinator Rebalances<br/>Segments]
    Rebalance2[Coordinator Rebalances<br/>Segments]
    
    SW1_T0 --> Rebalance1
    SW2_T0 --> Rebalance1
    Rebalance1 --> SW1_T1
    Rebalance1 --> SW2_T1
    Rebalance1 --> SW3_T1
    
    SW1_T1 --> Rebalance2
    SW2_T1 --> Rebalance2
    SW3_T1 --> Rebalance2
    Rebalance2 --> SW1_T2
    Rebalance2 --> SW2_T2
    
    style Rebalance1 fill:#fff59d
    style Rebalance2 fill:#fff59d
```

## How to View These Diagrams

These diagrams use Mermaid syntax and can be viewed in:
- **GitHub**: Automatically renders Mermaid diagrams
- **VS Code**: Use the Mermaid Preview extension
- **Online**: [Mermaid Live Editor](https://mermaid.live/)
- **IntelliJ IDEA**: Use Mermaid plugin

## Diagram Legend

- **Blue**: Core services and components
- **Yellow**: Decision points and coordinators
- **Green**: Success paths and active states
- **Red**: Error paths and failures
- **Purple**: Worker execution components
- **Orange**: Monitoring and observability
