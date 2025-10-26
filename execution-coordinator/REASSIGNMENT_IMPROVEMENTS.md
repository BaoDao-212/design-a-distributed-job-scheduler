# Job Reassignment Improvements

## 📋 Overview

Hàm `reassignJobsFromWorker` đã được improve với logic đầy đủ để xử lý job reassignment khi worker failures xảy ra.

## 🎯 Key Improvements

### 1. **Query RUNNING Jobs Only**
```java
List<ExecutionAssignmentEntity> runningAssignments = 
    executionAssignmentRepository.findByWorkerIdAndStatus(failedWorkerId, JobStatus.RUNNING);
```
**Benefits:**
- Chỉ reassign jobs đang thực thi
- Tránh reassign jobs đã COMPLETED hoặc FAILED
- Giảm overhead không cần thiết

### 2. **Smart Worker Selection**
```java
private Optional<ExecutionWorkerEntity> selectBestWorkerForReassignment() {
    return healthyWorkers.stream()
        .filter(w -> w.getCurrentLoad() < w.getCapacity())  // Has capacity
        .filter(w -> Duration.between(w.getLastHeartbeat(), Instant.now()).getSeconds() < 45)  // Recent heartbeat
        .min(Comparator.comparing(w -> w.getCurrentLoad() / w.getCapacity()))  // Lowest load %
        .orElse(null);
}
```

**Selection Criteria:**
- ✅ Worker status = ONLINE
- ✅ Has available capacity (currentLoad < capacity)
- ✅ Recent heartbeat (< 45 seconds)
- ✅ Lowest load percentage (load balancing)

**Example:**
```
Worker A: load=8/10  → 80% utilization
Worker B: load=3/10  → 30% utilization ✓ Selected
Worker C: load=5/10  → 50% utilization
```

### 3. **Checkpoint-Based Resume**
```java
JobDispatchEvent dispatchEvent = JobDispatchEvent.builder()
    .jobId(jobId)
    .payload(checkpointData)  // Resume from checkpoint
    .scheduledTime(Instant.now())
    .maxRetries(MAX_REASSIGNMENT_ATTEMPTS)
    .build();
```

**Benefits:**
- Long-running jobs can resume from last checkpoint
- Avoid repeating completed work
- Save computation resources
- Reduce execution time

### 4. **Atomic Assignment Update**
```java
if (dispatchSuccess) {
    // Update assignment
    assignment.setWorkerId(targetWorkerId);
    assignment.setStatus(JobStatus.RUNNING);
    executionAssignmentRepository.save(assignment);
    
    // Update worker load
    targetWorker.setCurrentLoad(targetWorker.getCurrentLoad() + 1);
    executionWorkerRepository.save(targetWorker);
}
```

**Ensures:**
- Database consistency
- Accurate load tracking
- Assignment history maintained

### 5. **Comprehensive Event Tracking**
```java
private void recordReassignmentEvent(Long jobId, String fromWorkerId, String toWorkerId,
                                      String checkpointData, boolean success, String errorMessage) {
    ReassignmentEventEntity event = new ReassignmentEventEntity();
    event.setJobId(jobId);
    event.setFromWorkerId(fromWorkerId);
    event.setToWorkerId(toWorkerId);
    event.setReason("Worker failure detected");
    event.setCheckpointData(checkpointData);
    event.setSuccess(success);
    event.setErrorMessage(errorMessage);
    reassignmentEventRepository.save(event);
}
```

**Benefits:**
- Complete audit trail
- Debugging support
- Analytics and metrics
- SLA tracking

### 6. **Error Handling**
```java
for (ExecutionAssignmentEntity assignment : runningAssignments) {
    try {
        reassignJob(assignment, failedWorkerId);
    } catch (Exception e) {
        log.error("Failed to reassign job {} from worker {}: {}", 
            assignment.getJobId(), failedWorkerId, e.getMessage(), e);
        // Continue with next job - don't fail entire batch
    }
}
```

**Robustness:**
- Individual job failure doesn't stop others
- All errors logged with context
- Graceful degradation

### 7. **No Worker Available Handling**
```java
if (targetWorkerOpt.isEmpty()) {
    log.error("No healthy worker available for reassigning job {}", jobId);
    recordReassignmentEvent(jobId, failedWorkerId, null, 
        checkpointData, false, "No healthy worker available");
    return;
}
```

**Fallback:**
- Job remains in queue
- Event recorded for monitoring
- Retry on next health check cycle
- Alert can be triggered

## 🏗️ Architecture Components

### New Entities

#### ReassignmentEventEntity
```java
@Entity
@Table(name = "reassignment_events")
public class ReassignmentEventEntity {
    private Long id;
    private Long jobId;
    private String fromWorkerId;
    private String toWorkerId;
    private String reason;
    private String checkpointData;
    private Boolean success;
    private String errorMessage;
    private Instant createdAt;
}
```

**Purpose:** Track all reassignment attempts with complete history

### New Services

#### WorkerAgentClient
```java
@Component
public class WorkerAgentClient {
    public boolean dispatchJob(String workerId, JobDispatchEvent event);
    private String getWorkerUrl(String workerId);
}
```

**Purpose:** 
- Encapsulate Worker Agent communication
- Handle dynamic URL resolution
- Retry logic (future enhancement)

### Enhanced Repositories

#### ExecutionAssignmentRepository
```java
List<ExecutionAssignmentEntity> findByWorkerIdAndStatus(String workerId, JobStatus status);
```

**Purpose:** Efficiently query RUNNING jobs for specific worker

## 📊 Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Worker Failure Detected                      │
│                     (60s heartbeat timeout)                     │
└────────────────────────┬────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│              Query RUNNING Jobs from Failed Worker              │
│  findByWorkerIdAndStatus(failedWorkerId, RUNNING)              │
└────────────────────────┬────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                  For Each Running Job:                          │
└────────────────────────┬────────────────────────────────────────┘
                         ↓
        ┌────────────────┴────────────────┐
        ↓                                  ↓
┌─────────────────┐              ┌─────────────────────┐
│ Load Checkpoint │              │ Select Best Worker  │
│      Data       │              │ (Lowest Load %)     │
└────────┬────────┘              └──────────┬──────────┘
         │                                   │
         └──────────────┬────────────────────┘
                        ↓
         ┌──────────────────────────────────┐
         │    Create Dispatch Event         │
         │  (with checkpoint data)          │
         └──────────────┬───────────────────┘
                        ↓
         ┌──────────────────────────────────┐
         │   Dispatch to New Worker via     │
         │   WorkerAgentClient              │
         └──────────────┬───────────────────┘
                        ↓
              ┌─────────┴─────────┐
              ↓                   ↓
    ┌─────────────────┐   ┌─────────────────┐
    │    SUCCESS      │   │     FAILURE     │
    └────────┬────────┘   └────────┬────────┘
             ↓                     ↓
    ┌─────────────────┐   ┌─────────────────┐
    │ Update          │   │ Record Event    │
    │ Assignment      │   │ (success=false) │
    │ & Worker Load   │   │                 │
    └────────┬────────┘   └────────┬────────┘
             │                     │
             └──────────┬──────────┘
                        ↓
         ┌──────────────────────────────────┐
         │    Record Reassignment Event     │
         │    (audit trail)                 │
         └──────────────────────────────────┘
```

## 🧪 Testing Scenarios

### Scenario 1: Successful Reassignment
```bash
# Setup: 2 workers, 1 job running on worker-1
curl -X POST http://localhost:8085/api/execution-coordinator/workers/worker-1 \
  -d "capacity=10&host=localhost&port=8084"

curl -X POST http://localhost:8085/api/execution-coordinator/workers/worker-2 \
  -d "capacity=10&host=localhost&port=8184"

# Simulate: worker-1 stops sending heartbeat
# Wait 60+ seconds

# Expected:
# - Worker-1 marked UNHEALTHY
# - Job reassigned to worker-2
# - Assignment updated in database
# - ReassignmentEvent created
# - Worker-2 load incremented
```

### Scenario 2: No Worker Available
```bash
# Setup: Only 1 worker at full capacity
curl -X POST http://localhost:8085/api/execution-coordinator/workers/worker-1 \
  -d "capacity=10"

# Set currentLoad = 10 (full)
curl -X POST http://localhost:8085/api/execution-coordinator/workers/worker-1/heartbeat \
  -d "currentLoad=10"

# Simulate: worker-1 fails
# Expected:
# - No healthy worker with capacity
# - ReassignmentEvent with success=false
# - Error message: "No healthy worker available"
# - Job remains in queue for next cycle
```

### Scenario 3: Load Balancing
```bash
# Setup: 3 workers with different loads
curl -X POST http://localhost:8085/api/execution-coordinator/workers/worker-1/heartbeat \
  -d "currentLoad=8&capacity=10"  # 80%

curl -X POST http://localhost:8085/api/execution-coordinator/workers/worker-2/heartbeat \
  -d "currentLoad=3&capacity=10"  # 30% ← Should be selected

curl -X POST http://localhost:8085/api/execution-coordinator/workers/worker-3/heartbeat \
  -d "currentLoad=5&capacity=10"  # 50%

# Expected:
# - Worker-2 selected (lowest load %)
# - Job dispatched to worker-2
```

## 📈 Monitoring Queries

### View Recent Reassignments
```sql
SELECT * FROM reassignment_events 
ORDER BY created_at DESC 
LIMIT 20;
```

### Reassignment Success Rate
```sql
SELECT 
    COUNT(*) as total_reassignments,
    SUM(CASE WHEN success = true THEN 1 ELSE 0 END) as successful,
    AVG(CASE WHEN success = true THEN 1.0 ELSE 0.0 END) * 100 as success_rate_percent
FROM reassignment_events
WHERE created_at > NOW() - INTERVAL '24 HOURS';
```

### Most Problematic Workers
```sql
SELECT 
    from_worker_id,
    COUNT(*) as failure_count,
    MAX(created_at) as last_failure
FROM reassignment_events
WHERE created_at > NOW() - INTERVAL '7 DAYS'
GROUP BY from_worker_id
ORDER BY failure_count DESC;
```

### Jobs Reassigned Multiple Times
```sql
SELECT 
    job_id,
    COUNT(*) as reassignment_count,
    MAX(created_at) as last_reassignment
FROM reassignment_events
GROUP BY job_id
HAVING COUNT(*) > 1
ORDER BY reassignment_count DESC;
```

## ⚡ Performance Considerations

### Database Indexes
```sql
-- ExecutionAssignmentEntity
CREATE INDEX idx_assignment_worker_status ON execution_assignments(worker_id, status);
CREATE INDEX idx_assignment_job ON execution_assignments(job_id);

-- ExecutionWorkerEntity
CREATE INDEX idx_worker_status ON execution_workers(status);
CREATE INDEX idx_worker_heartbeat ON execution_workers(last_heartbeat);

-- ReassignmentEventEntity
CREATE INDEX idx_reassignment_job ON reassignment_events(job_id);
CREATE INDEX idx_reassignment_created ON reassignment_events(created_at);
```

### Batch Processing
Current: Process jobs sequentially
Future: Consider parallel processing with CompletableFuture

```java
List<CompletableFuture<Void>> futures = runningAssignments.stream()
    .map(assignment -> CompletableFuture.runAsync(() -> 
        reassignJob(assignment, failedWorkerId)
    ))
    .collect(Collectors.toList());

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

## 🔮 Future Enhancements

### 1. Retry with Exponential Backoff
```java
@Retryable(
    value = {RestClientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public boolean dispatchJob(String workerId, JobDispatchEvent event) {
    // Dispatch logic
}
```

### 2. Priority-Based Reassignment
```java
// Reassign high-priority jobs first
runningAssignments.sort(Comparator.comparing(ExecutionAssignmentEntity::getPriority).reversed());
```

### 3. Circuit Breaker Pattern
```java
@CircuitBreaker(name = "workerAgent", fallbackMethod = "dispatchFallback")
public boolean dispatchJob(String workerId, JobDispatchEvent event) {
    // Dispatch logic
}
```

### 4. Kafka-Based Dispatch
Replace REST with async Kafka messages for better scalability

### 5. Dead Letter Queue
Jobs that fail reassignment multiple times → DLQ for manual intervention

## ⚠️ Important Notes

1. **Transaction Boundary**: `monitorWorkerHealth()` is @Transactional
2. **Checkpoint Data**: Ensure Worker Agent supports checkpoint resume
3. **Load Tracking**: Worker must report accurate currentLoad via heartbeat
4. **Timeout Configuration**: 60s timeout should be > 2x heartbeat interval
5. **Database Locking**: Consider pessimistic locking for high concurrency

## 📚 Related Documentation

- [Execution Coordinator README](./README.md)
- [System Design](../SYSTEM_DESIGN.md)
- [Worker Agent README](../worker-agent/README.md)

---

**Implementation Status**: ✅ Complete
**Test Coverage**: Requires integration tests
**Production Ready**: Requires load testing and monitoring setup
