# Distributed Job Scheduler - System Design and Implementation

## Overview

This project implements a scalable distributed job scheduling system using Spring Boot microservices architecture. The system can handle millions of tasks, ensure high availability, and prevent single points of failure.

## Architecture

The system consists of 5 main microservices:

### 1. Job Store Service (Port 8081)
- **Purpose**: Centralized persistence layer for job and worker state
- **Responsibilities**:
  - Store job metadata (status, frequency, payload, execution time)
  - Maintain job schedules with next_run_time
  - Track job execution history
  - Store worker registration and heartbeat data
- **Key Entities**:
  - `JobEntity`: Stores job information
  - `JobScheduleEntity`: Stores scheduling data with segments
  - `JobExecutionEntity`: Tracks execution attempts
  - `WorkerEntity`: Stores worker metadata
  - `WorkerHeartbeatEntity`: Tracks worker health

### 2. Scheduler Coordinator (Port 8082)
- **Purpose**: Orchestrates scheduler worker nodes
- **Responsibilities**:
  - Leader election among coordinator nodes (prevents single point of failure)
  - Assign segments to scheduler workers
  - Monitor scheduler worker health via heartbeats
  - Rebalance segments when workers join/leave
- **Key Features**:
  - Multiple coordinator nodes with leader election
  - Dynamic segment assignment (0-99 segments)
  - Health monitoring and failover

### 3. Scheduler Worker (Port 8083)
- **Purpose**: Scans job schedules and dispatches jobs to execution queue
- **Responsibilities**:
  - Poll `job_schedules` table by `next_run_time` + assigned segments
  - Publish jobs to dispatch queue
  - Update job status to SCHEDULED
  - Register with coordinator for segment assignment
- **Key Features**:
  - Configurable segment assignment
  - Periodic scanning (every 60 seconds)
  - Idempotent job dispatching

### 4. Worker Agent (Port 8084)
- **Purpose**: Executes actual jobs
- **Responsibilities**:
  - Consume job dispatch events
  - Execute jobs with concurrency limits
  - Support checkpointing for long-running jobs
  - Update job status (RUNNING, COMPLETED, FAILED)
- **Key Features**:
  - Semaphore-based concurrency control
  - Configurable concurrency limit (default: 10)
  - Job execution simulation

### 5. Execution Coordinator (Port 8085)
- **Purpose**: Manages execution worker lifecycle
- **Responsibilities**:
  - Monitor worker agent heartbeats and capacity
  - Detect failed workers (no heartbeat for 60 seconds)
  - Reassign jobs from failed workers
  - Track execution assignments with checkpoint data
- **Key Features**:
  - Health monitoring every 30 seconds
  - Automatic worker failover
  - Job reassignment on worker failure

## System Flow

```
1. Job Submission
   Client → Job Store Service → Creates Job + Schedule

2. Job Scheduling
   Scheduler Worker → Job Store Service → Get due jobs for segments
   Scheduler Worker → Publishes job dispatch event
   Scheduler Worker → Updates job status to SCHEDULED

3. Job Execution
   Worker Agent → Receives job dispatch event
   Worker Agent → Executes job (with concurrency control)
   Worker Agent → Updates job status (RUNNING → COMPLETED/FAILED)

4. Health Monitoring
   Execution Coordinator → Monitors worker heartbeats
   Execution Coordinator → Detects failures → Reassigns jobs
```

## Key Design Patterns

### 1. Segment-Based Partitioning
- Total 100 segments (0-99)
- Each job schedule assigned to a segment
- Scheduler workers process specific segments
- Prevents duplicate job dispatching
- Enables horizontal scaling

### 2. Leader Election
- Multiple coordinator nodes
- One active leader at a time
- Automatic failover on leader failure
- Prevents single point of failure

### 3. Heartbeat Mechanism
- Workers send periodic heartbeats
- Coordinators track last heartbeat time
- Workers marked UNHEALTHY after timeout
- Enables automatic failure detection

### 4. Concurrency Control
- Semaphore-based limiting in Worker Agent
- Prevents resource exhaustion
- Configurable per worker

### 5. Idempotency
- Dispatch records tracked
- Job status transitions managed
- Prevents duplicate job execution

## Database Schema

### Jobs Table
```sql
- id (PK)
- job_name
- user_id
- frequency (ONE_TIME, DAILY, WEEKLY, MONTHLY)
- payload (TEXT)
- execution_time
- retry_count
- max_retries
- status (PENDING, SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED)
- created_at, updated_at
```

### Job Schedules Table
```sql
- id (PK)
- job_id (FK)
- next_run_time
- last_run_time
- segment (0-99)
```

### Job Executions Table
```sql
- id (PK)
- job_id (FK)
- worker_id
- start_time, end_time
- status
- error_message
- checkpoint_data (TEXT)
- created_at
```

### Workers Table
```sql
- id (PK)
- worker_id (unique)
- status (ONLINE, OFFLINE, UNHEALTHY)
- segment
- capacity
- current_load
- created_at, updated_at
```

## API Endpoints

### Job Store Service (8081)
```
POST   /api/jobs                  - Submit new job
GET    /api/jobs/{id}             - Get job by ID
GET    /api/jobs?status=          - Get jobs by status
DELETE /api/jobs/{id}             - Cancel job
PUT    /api/jobs/{id}/status      - Update job status
GET    /api/job-schedules/due     - Get due schedules for segments
POST   /api/workers               - Register worker
GET    /api/workers               - List all workers
```

### Scheduler Coordinator (8082)
```
POST   /api/coordinator/register  - Register coordinator node
GET    /api/coordinator/leader    - Get current leader
POST   /api/coordinator/heartbeat - Send coordinator heartbeat
POST   /api/segments/assign       - Assign segments to worker
DELETE /api/segments/release      - Release worker segments
POST   /api/scheduler-workers/register - Register scheduler worker
POST   /api/scheduler-workers/heartbeat - Scheduler worker heartbeat
GET    /api/scheduler-workers     - List active scheduler workers
```

### Scheduler Worker (8083)
```
POST   /api/scheduler-worker/segments - Update assigned segments
GET    /api/scheduler-worker/segments - Get assigned segments
```

### Worker Agent (8084)
```
POST   /api/worker-agent/dispatch - Execute job
```

### Execution Coordinator (8085)
```
POST   /api/execution-coordinator/workers/{workerId} - Register execution worker
POST   /api/execution-coordinator/workers/{workerId}/heartbeat - Send heartbeat
GET    /api/execution-coordinator/workers - List all workers
```

## Configuration

Each service uses H2 in-memory database for development. For production:

1. Update `application.yml` to use PostgreSQL
2. Configure connection pools appropriately
3. Set proper concurrency limits
4. Configure segment assignments

## Non-Functional Requirements Addressed

### Scalability
- Horizontal scaling via segment partitioning
- Multiple coordinator nodes
- Multiple scheduler workers
- Multiple worker agents
- Each handles specific job segments

### High Availability
- No single point of failure
- Leader election for coordinators
- Worker health monitoring
- Automatic job reassignment on failure

### Consistency
- Idempotent job dispatching
- Segment-based job ownership
- Status tracking for each execution
- Checkpoint support for retries

### Latency
- Periodic scanning (60s for scheduling)
- Health monitoring (30s for execution)
- Configurable concurrency per worker
- Direct REST API communication

## Building and Running

### Build All Services
```bash
cd /home/engine/project
mvn clean install
```

### Run Individual Services
```bash
# Job Store Service
cd job-store-service
mvn spring-boot:run

# Scheduler Coordinator
cd scheduler-coordinator
mvn spring-boot:run

# Scheduler Worker
cd scheduler-worker
mvn spring-boot:run

# Worker Agent
cd worker-agent
mvn spring-boot:run

# Execution Coordinator
cd execution-coordinator
mvn spring-boot:run
```

## Testing the System

### 1. Submit a Job
```bash
curl -X POST http://localhost:8081/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "Test Job",
    "userId": 1,
    "frequency": "ONE_TIME",
    "executionTime": "2024-12-31T10:00:00Z",
    "payload": "Job payload data",
    "maxRetries": 3,
    "segment": 5
  }'
```

### 2. Register Scheduler Worker
```bash
curl -X POST "http://localhost:8082/api/scheduler-workers/register?workerId=worker-1"
```

### 3. Assign Segments
```bash
curl -X POST "http://localhost:8082/api/segments/assign?workerId=worker-1&desiredSegments=10"
```

### 4. Check Job Status
```bash
curl http://localhost:8081/api/jobs/1
```

## Future Enhancements

1. **Kafka Integration**: Replace REST with Kafka for job dispatch events
2. **Job Prioritization**: Add priority queue for important jobs
3. **Job Dependencies**: Support workflows with dependent jobs
4. **Rate Limiting**: Implement client-level and queue-level throttling
5. **Observability**: Add Prometheus metrics and distributed tracing
6. **Job Checkpointing**: Full checkpoint/resume implementation
7. **Backpressure**: Handle worker overload gracefully
8. **Dead Letter Queue**: Handle permanently failed jobs

## Technology Stack

- **Spring Boot 3.5.5**: Application framework
- **Java 21**: Programming language
- **H2 Database**: In-memory database (dev)
- **PostgreSQL**: Production database (recommended)
- **Maven**: Build tool
- **Lombok**: Code generation
- **MapStruct**: Object mapping

## Monitoring and Operations

- Check worker health via heartbeat APIs
- Monitor job status transitions
- Track segment distribution
- Review execution logs
- Alert on worker failures
