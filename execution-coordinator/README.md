# Execution Coordinator

**Port:** 8085  
**Role:** Worker Health Monitor & Job Reassignment

## 📖 Tổng Quan (Overview)

Execution Coordinator là service giám sát sức khỏe của Worker Agents và xử lý job reassignment khi worker failures xảy ra. Service này đảm bảo không có job nào bị mất khi worker agents gặp sự cố.

Execution Coordinator is the service that monitors Worker Agent health and handles job reassignment when worker failures occur. This service ensures no jobs are lost when worker agents fail.

## 🎯 Trách Nhiệm (Responsibilities)

- **Health Monitoring**: Theo dõi heartbeat của Worker Agents
- **Failure Detection**: Phát hiện workers không hoạt động (timeout 60s)
- **Job Reassignment**: Reassign jobs từ failed workers sang healthy workers
- **Worker Registration**: Quản lý Worker Agent registration
- **Load Balancing**: Phân phối jobs dựa trên worker capacity
- **Execution Tracking**: Theo dõi job assignments với checkpoint data

---

- **Health Monitoring**: Track Worker Agent heartbeats
- **Failure Detection**: Detect inactive workers (60s timeout)
- **Job Reassignment**: Reassign jobs from failed workers to healthy workers
- **Worker Registration**: Manage Worker Agent registration
- **Load Balancing**: Distribute jobs based on worker capacity
- **Execution Tracking**: Track job assignments with checkpoint data

## 🏗️ Architecture Components

### 1. Heartbeat Monitor

```java
@Scheduled(fixedRate = 30000) // Every 30 seconds
public void monitorWorkerHealth() {
    // 1. Get all registered workers
    // 2. Check last_heartbeat_time
    // 3. If (now - last_heartbeat > 60s):
    //    - Mark worker as UNHEALTHY
    //    - Trigger job reassignment
}
```

### 2. Job Reassignment Engine

```java
public void reassignJobsFromFailedWorker(String failedWorkerId) {
    // 1. Get all RUNNING jobs assigned to failed worker
    // 2. Get available healthy workers
    // 3. For each job:
    //    a. Select worker with lowest load
    //    b. Load checkpoint data
    //    c. Reassign job to new worker
    //    d. Update execution assignment
    //    e. Dispatch to new worker
}
```

### 3. Worker Registry

```java
public class WorkerRegistry {
    // Maintains:
    // - Worker ID → Worker metadata
    // - Worker status (ONLINE/UNHEALTHY/OFFLINE)
    // - Last heartbeat timestamp
    // - Current capacity and load
    // - Assigned jobs
}
```

### 4. Execution Assignment Tracker

```java
public class ExecutionAssignment {
    // Tracks:
    // - Job ID → Worker ID mapping
    // - Assignment timestamp
    // - Checkpoint data
    // - Reassignment history
}
```

## 🔌 API Endpoints

### Worker Management

#### Register Worker Agent
```bash
POST /api/execution-coordinator/workers/{workerId}
Content-Type: application/json

{
  "workerId": "agent-1",
  "host": "localhost",
  "port": 8084,
  "capacity": 10
}
```

Response:
```json
{
  "success": true,
  "data": {
    "workerId": "agent-1",
    "status": "ONLINE",
    "registeredAt": "2024-12-31T10:00:00Z"
  }
}
```

#### Worker Heartbeat
```bash
POST /api/execution-coordinator/workers/{workerId}/heartbeat
Content-Type: application/json

{
  "currentLoad": 5,
  "capacity": 10,
  "status": "ONLINE"
}
```

Response:
```json
{
  "success": true,
  "data": "OK"
}
```

#### List All Workers
```bash
GET /api/execution-coordinator/workers
```

Response:
```json
{
  "success": true,
  "data": [
    {
      "workerId": "agent-1",
      "status": "ONLINE",
      "capacity": 10,
      "currentLoad": 5,
      "lastHeartbeat": "2024-12-31T10:00:00Z"
    },
    {
      "workerId": "agent-2",
      "status": "UNHEALTHY",
      "capacity": 10,
      "currentLoad": 8,
      "lastHeartbeat": "2024-12-31T09:58:00Z"
    }
  ]
}
```

#### Get Worker Details
```bash
GET /api/execution-coordinator/workers/{workerId}
```

#### Deregister Worker (Graceful Shutdown)
```bash
DELETE /api/execution-coordinator/workers/{workerId}
```

### Execution Assignment

#### Get Job Assignment
```bash
GET /api/execution-coordinator/assignments/{jobId}
```

Response:
```json
{
  "success": true,
  "data": {
    "jobId": 123,
    "workerId": "agent-1",
    "assignedAt": "2024-12-31T10:00:00Z",
    "checkpointData": "{ \"progress\": 50 }"
  }
}
```

#### Get Worker Assignments
```bash
GET /api/execution-coordinator/assignments?workerId=agent-1
```

### Monitoring

#### Get System Health
```bash
GET /api/execution-coordinator/health
```

Response:
```json
{
  "success": true,
  "data": {
    "totalWorkers": 5,
    "onlineWorkers": 4,
    "unhealthyWorkers": 1,
    "totalCapacity": 50,
    "usedCapacity": 35,
    "reassignmentEvents": 12
  }
}
```

## 🔄 Health Monitoring Flow

```
1. Worker Agent sends heartbeat (every 30 seconds)
   POST /api/execution-coordinator/workers/{workerId}/heartbeat
   {
     "currentLoad": 5,
     "capacity": 10,
     "status": "ONLINE"
   }

2. Execution Coordinator updates last_heartbeat_time
   UPDATE workers 
   SET last_heartbeat_time = NOW(), 
       current_load = 5,
       status = 'ONLINE'
   WHERE worker_id = 'agent-1'

3. Health Monitor checks periodically (every 30 seconds)
   SELECT * FROM workers 
   WHERE last_heartbeat_time < (NOW() - INTERVAL '60 seconds')

4. If worker timeout detected:
   a. Mark worker as UNHEALTHY
   b. Get all jobs assigned to this worker
   c. Trigger job reassignment

5. Job Reassignment Process:
   a. Query: SELECT * FROM job_executions 
            WHERE worker_id = 'failed-worker' 
            AND status = 'RUNNING'
   
   b. For each running job:
      - Load checkpoint data
      - Find healthy worker with capacity
      - Update job assignment
      - Dispatch to new worker with checkpoint
   
   c. Log reassignment event
```

## 🚀 How to Run

### Prerequisites
- Java 21
- Maven 3.6+
- Job Store Service running (port 8081)
- Worker Agent(s) running (port 8084)

### Build
```bash
cd execution-coordinator
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Service sẽ chạy trên **http://localhost:8085**

### Run with Custom Configuration
```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--execution.coordinator.heartbeat-timeout=60000"
```

## ⚙️ Configuration

```yaml
server:
  port: 8085

spring:
  application:
    name: execution-coordinator

execution:
  coordinator:
    heartbeat-check-interval: 30000  # 30 seconds
    heartbeat-timeout: 60000         # 60 seconds
    reassignment-enabled: true
    max-reassignment-attempts: 3
    job-store-url: http://localhost:8081
```

## 🔑 Key Features

### 1. Automatic Failure Detection

```
Timeline:
T=0s   : Worker sends heartbeat ✓
T=30s  : Worker sends heartbeat ✓
T=60s  : Worker sends heartbeat ✓
T=90s  : Worker crash ✗
T=120s : Coordinator checks - last heartbeat at T=60s
         (120 - 60 = 60s > timeout)
         → Mark UNHEALTHY
         → Trigger reassignment
```

### 2. Smart Job Reassignment

```java
// Select best worker for reassignment
Worker selectWorker(List<Worker> healthyWorkers) {
    return healthyWorkers.stream()
        .filter(w -> w.getStatus() == ONLINE)
        .filter(w -> w.getCurrentLoad() < w.getCapacity())
        .min(Comparator.comparing(w -> 
            w.getCurrentLoad() / (double) w.getCapacity()
        ))
        .orElse(null);
}
```

**Criteria:**
- Worker must be ONLINE
- Worker must have available capacity
- Prefer worker with lowest load %
- Round-robin if multiple workers equal

### 3. Checkpoint-Based Resume

```java
// Reassign with checkpoint
JobDispatchEvent createReassignmentEvent(Job job, String checkpointData) {
    return JobDispatchEvent.builder()
        .jobId(job.getId())
        .jobName(job.getJobName())
        .payload(job.getPayload())
        .checkpointData(checkpointData)  // Resume from checkpoint
        .isReassignment(true)
        .previousWorkerId(job.getWorkerId())
        .build();
}
```

### 4. Reassignment History Tracking

```java
@Entity
public class ReassignmentEvent {
    private Long jobId;
    private String fromWorkerId;
    private String toWorkerId;
    private Instant reassignedAt;
    private String reason;
    private String checkpointData;
}
```

## 📊 Service Dependencies

```
Execution Coordinator ← Worker Agent (heartbeat)
Execution Coordinator → Worker Agent (job reassignment)
Execution Coordinator → Job Store Service (query jobs, update status)
```

### Required Services
1. **Job Store Service** (8081): Job and execution data
2. **Worker Agent** (8084): Target for reassignment

## 🧪 Testing Scenarios

### 1. Normal Heartbeat Test

```bash
# Register worker
curl -X POST http://localhost:8085/api/execution-coordinator/workers/agent-1 \
  -H "Content-Type: application/json" \
  -d '{
    "workerId": "agent-1",
    "host": "localhost",
    "port": 8084,
    "capacity": 10
  }'

# Send heartbeat every 30 seconds
while true; do
  curl -X POST http://localhost:8085/api/execution-coordinator/workers/agent-1/heartbeat \
    -H "Content-Type: application/json" \
    -d '{
      "currentLoad": 5,
      "capacity": 10,
      "status": "ONLINE"
    }'
  sleep 30
done

# Check worker status
curl http://localhost:8085/api/execution-coordinator/workers/agent-1
```

### 2. Worker Failure & Reassignment Test

```bash
# Step 1: Register two workers
curl -X POST http://localhost:8085/api/execution-coordinator/workers/agent-1 \
  -H "Content-Type: application/json" \
  -d '{"workerId": "agent-1", "host": "localhost", "port": 8084, "capacity": 10}'

curl -X POST http://localhost:8085/api/execution-coordinator/workers/agent-2 \
  -H "Content-Type: application/json" \
  -d '{"workerId": "agent-2", "host": "localhost", "port": 8184, "capacity": 10}'

# Step 2: Dispatch job to agent-1
# (via Scheduler Worker)

# Step 3: Verify job is running on agent-1
curl http://localhost:8081/api/executions?workerId=agent-1

# Step 4: Stop agent-1 heartbeat (simulate crash)
# Stop sending heartbeats

# Step 5: Wait 60+ seconds

# Step 6: Check worker status (should be UNHEALTHY)
curl http://localhost:8085/api/execution-coordinator/workers/agent-1

# Step 7: Check job reassignment (should be on agent-2)
curl http://localhost:8081/api/executions?jobId={id}
# workerId should change from agent-1 to agent-2
```

### 3. Load Balancing Test

```bash
# Register 3 workers with different loads
curl -X POST http://localhost:8085/api/execution-coordinator/workers/agent-1/heartbeat \
  -H "Content-Type: application/json" \
  -d '{"currentLoad": 8, "capacity": 10, "status": "ONLINE"}'  # 80% load

curl -X POST http://localhost:8085/api/execution-coordinator/workers/agent-2/heartbeat \
  -H "Content-Type: application/json" \
  -d '{"currentLoad": 3, "capacity": 10, "status": "ONLINE"}'  # 30% load

curl -X POST http://localhost:8085/api/execution-coordinator/workers/agent-3/heartbeat \
  -H "Content-Type: application/json" \
  -d '{"currentLoad": 5, "capacity": 10, "status": "ONLINE"}'  # 50% load

# Trigger reassignment
# New jobs should go to agent-2 (lowest load %)
```

### 4. Graceful Shutdown Test

```bash
# Worker announces shutdown
curl -X DELETE http://localhost:8085/api/execution-coordinator/workers/agent-1

# Coordinator should:
# 1. Mark worker as OFFLINE
# 2. Wait for running jobs to complete (or reassign immediately)
# 3. Remove from available workers pool
```

## 📈 Monitoring

### Key Metrics

1. **Worker Health**
   - Online workers count
   - Unhealthy workers count
   - Average heartbeat latency
   - Heartbeat miss rate

2. **Reassignment Stats**
   - Total reassignments
   - Reassignments per hour
   - Reassignment success rate
   - Average reassignment time

3. **Capacity Utilization**
   - Total system capacity
   - Used capacity
   - Available capacity
   - Per-worker utilization %

4. **Failure Detection**
   - Worker failure detection time
   - False positive rate
   - Time to reassignment

### Health Dashboard
```bash
# System overview
curl http://localhost:8085/api/execution-coordinator/health

# Worker list
curl http://localhost:8085/api/execution-coordinator/workers

# Reassignment history
curl http://localhost:8085/api/execution-coordinator/reassignments?limit=20
```

## 🐛 Troubleshooting

### Issue: Workers Marked as UNHEALTHY Incorrectly

**Possible Causes:**
1. Network latency
2. Heartbeat timeout too aggressive
3. Worker overloaded (can't send heartbeat)

**Solutions:**
```yaml
# Increase heartbeat timeout
execution:
  coordinator:
    heartbeat-timeout: 90000  # 90 seconds instead of 60
```

### Issue: Jobs Not Being Reassigned

**Possible Causes:**
1. No healthy workers available
2. All workers at capacity
3. Reassignment disabled in config
4. Job Store Service unavailable

**Solutions:**
```bash
# Check available workers
curl http://localhost:8085/api/execution-coordinator/workers

# Check reassignment configuration
# Enable reassignment in application.yml

# Check Job Store Service
curl http://localhost:8081/actuator/health
```

### Issue: Repeated Reassignments (Ping-Pong)

**Possible Causes:**
1. Worker comes back online quickly
2. Network flapping
3. Insufficient timeout margin

**Solutions:**
```yaml
# Increase timeout and add grace period
execution:
  coordinator:
    heartbeat-timeout: 90000
    reassignment-grace-period: 30000  # Wait 30s before reassigning
```

## 📝 Important Notes

1. **Heartbeat Timeout**: Should be at least 2x heartbeat interval
2. **Database Consistency**: Use transactions for reassignment
3. **Checkpoint Data**: Critical for resuming long-running jobs
4. **Load Balancing**: Consider worker capacity and current load
5. **False Positives**: Network issues can cause false failures
6. **Graceful Shutdown**: Workers should deregister before stopping
7. **Monitoring**: Alert on high reassignment rate

## ⚠️ Critical Rules

**MANDATORY**:
- ✅ Check heartbeat timeout before marking UNHEALTHY
- ✅ Validate worker capacity before reassignment
- ✅ Load checkpoint data for reassigned jobs
- ✅ Update job execution records atomically
- ✅ Log all reassignment events for audit
- ✅ Handle reassignment failures gracefully
- ❌ KHÔNG reassign jobs với status != RUNNING
- ❌ KHÔNG reassign tới UNHEALTHY workers
- ❌ KHÔNG skip checkpoint data
- ⚠️ Use pessimistic locking for concurrent reassignments

## 🔗 Related Documentation

- [System Design](../SYSTEM_DESIGN.md) - Overall architecture
- [Development Guide](../DEVELOPMENT_GUIDE.md) - Development standards
- [Diagrams](../DIAGRAMS.md) - Health monitoring flow diagrams
- [Worker Agent README](../worker-agent/README.md) - Worker execution details

---

**For the main system documentation, see [../README.md](../README.md)**
