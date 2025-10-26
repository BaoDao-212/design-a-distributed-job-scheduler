# Worker Agent

**Port:** 8084  
**Role:** Job Executor

## 📖 Tổng Quan (Overview)

Worker Agent là service thực thi jobs trong hệ thống. Service này nhận job dispatch events từ Scheduler Worker, thực thi job logic với concurrency control, và cập nhật execution status về Job Store Service.

Worker Agent is the service that executes jobs in the system. It receives job dispatch events from Scheduler Worker, executes job logic with concurrency control, and updates execution status to Job Store Service.

## 🎯 Trách Nhiệm (Responsibilities)

- **Job Execution**: Thực thi job logic được dispatch từ Scheduler Worker
- **Concurrency Control**: Giới hạn số jobs chạy đồng thời (Semaphore-based)
- **Status Reporting**: Cập nhật job status (RUNNING, COMPLETED, FAILED)
- **Checkpoint Management**: Lưu checkpoint data cho long-running jobs
- **Error Handling**: Xử lý failures và retry logic
- **Heartbeat Reporting**: Gửi heartbeat tới Execution Coordinator

---

- **Job Execution**: Execute job logic dispatched from Scheduler Worker
- **Concurrency Control**: Limit concurrent job execution (Semaphore-based)
- **Status Reporting**: Update job status (RUNNING, COMPLETED, FAILED)
- **Checkpoint Management**: Save checkpoint data for long-running jobs
- **Error Handling**: Handle failures and retry logic
- **Heartbeat Reporting**: Send heartbeat to Execution Coordinator

## 🏗️ Architecture Components

### 1. Job Dispatcher Endpoint
```java
@PostMapping("/dispatch")
public ResponseData<String> dispatchJob(@RequestBody JobDispatchEvent event) {
    // 1. Validate job event
    // 2. Acquire semaphore (concurrency control)
    // 3. Execute job asynchronously
    // 4. Release semaphore when done
    return ResponseUtils.success("OK");
}
```

### 2. Job Executor
```java
@Async
public void executeJob(JobDispatchEvent event) {
    try {
        // 1. Update status to RUNNING
        updateJobStatus(jobId, JobStatus.RUNNING);
        
        // 2. Execute job logic
        String result = performJobExecution(event);
        
        // 3. Save checkpoint (if needed)
        saveCheckpoint(jobId, checkpoint);
        
        // 4. Update status to COMPLETED
        updateJobStatus(jobId, JobStatus.COMPLETED);
    } catch (Exception e) {
        // Update status to FAILED
        updateJobStatus(jobId, JobStatus.FAILED, e.getMessage());
    }
}
```

### 3. Concurrency Manager
- **Semaphore-based**: Giới hạn concurrent executions
- **Configurable Limit**: Default 10, có thể config
- **Fair Queuing**: FIFO queue cho waiting jobs
- **Graceful Degradation**: Reject jobs khi full capacity

### 4. Heartbeat Reporter
```java
@Scheduled(fixedRate = 30000) // Every 30 seconds
public void sendHeartbeat() {
    // Report to Execution Coordinator:
    // - Worker status (ONLINE/OFFLINE)
    // - Current load (running jobs count)
    // - Capacity available
}
```

## 🔌 API Endpoints

### Job Execution

#### Dispatch Job for Execution
```bash
POST /api/worker-agent/dispatch
Content-Type: application/json

{
  "jobId": 123,
  "jobName": "Example Job",
  "userId": 1,
  "payload": "Job data to process",
  "executionTime": "2024-12-31T10:00:00Z",
  "maxRetries": 3,
  "retryCount": 0
}
```

Response:
```json
{
  "success": true,
  "data": "OK"
}
```

**Status Codes:**
- `200 OK`: Job accepted for execution
- `503 Service Unavailable`: Worker at capacity, retry later
- `400 Bad Request`: Invalid job data

### Worker Status

#### Get Worker Status
```bash
GET /api/worker-agent/status
```

Response:
```json
{
  "success": true,
  "data": {
    "workerId": "agent-1",
    "status": "ONLINE",
    "capacity": 10,
    "currentLoad": 5,
    "availableSlots": 5,
    "totalJobsExecuted": 1523,
    "successfulJobs": 1498,
    "failedJobs": 25
  }
}
```

#### Get Running Jobs
```bash
GET /api/worker-agent/jobs/running
```

Response:
```json
{
  "success": true,
  "data": [
    {
      "jobId": 123,
      "jobName": "Long Running Job",
      "startTime": "2024-12-31T10:00:00Z",
      "elapsedTime": "00:05:30"
    },
    {
      "jobId": 456,
      "jobName": "Data Processing",
      "startTime": "2024-12-31T10:02:00Z",
      "elapsedTime": "00:03:30"
    }
  ]
}
```

## 🔄 Job Execution Flow

```
1. Receive job dispatch event from Scheduler Worker
   POST /api/worker-agent/dispatch

2. Validate job data
   - Check required fields
   - Validate job ID exists

3. Check capacity (Semaphore)
   if (availableSlots > 0) {
       Accept job
   } else {
       Return 503 (Service Unavailable)
   }

4. Acquire semaphore permit
   semaphore.acquire()

5. Create job execution record
   POST /api/job-store/executions
   {
     "jobId": 123,
     "workerId": "agent-1",
     "status": "RUNNING",
     "startTime": "2024-12-31T10:00:00Z"
   }

6. Update job status to RUNNING
   PUT /api/jobs/123/status?status=RUNNING

7. Execute job logic (asynchronously)
   - Parse payload
   - Perform business logic
   - Save checkpoint periodically
   
8. On Success:
   - Update job status to COMPLETED
   - Record end time
   - Log success metrics

9. On Failure:
   - Update job status to FAILED
   - Record error message
   - Check retry count
   - If retry available: reset to PENDING

10. Release semaphore permit
    semaphore.release()
```

## 🚀 How to Run

### Prerequisites
- Java 21
- Maven 3.6+
- Job Store Service running (port 8081)
- Scheduler Worker running (port 8083)

### Build
```bash
cd worker-agent
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Service sẽ chạy trên **http://localhost:8084**

### Run Multiple Agents (Horizontal Scaling)
```bash
# Terminal 1 - Agent 1 (default port 8084)
mvn spring-boot:run

# Terminal 2 - Agent 2 (port 8184)
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8184 --worker.agent.worker-id=agent-2"

# Terminal 3 - Agent 3 (port 8284)
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8284 --worker.agent.worker-id=agent-3"
```

## ⚙️ Configuration

```yaml
server:
  port: 8084

spring:
  application:
    name: worker-agent

worker:
  agent:
    worker-id: agent-1                      # Unique worker identifier
    concurrency-limit: 10                   # Max concurrent jobs
    job-store-url: http://localhost:8081    # Job Store Service URL
    heartbeat-interval: 30000               # Heartbeat interval (30 seconds)
    execution-timeout: 300000               # Max execution time (5 minutes)
    checkpoint-interval: 60000              # Checkpoint save interval (1 minute)
```

## 🔑 Key Features

### 1. Semaphore-Based Concurrency Control

```java
private final Semaphore semaphore = new Semaphore(concurrencyLimit, true);

public void executeJob(JobDispatchEvent event) {
    try {
        semaphore.acquire();  // Wait if no slots available
        
        // Execute job
        doJobExecution(event);
        
    } finally {
        semaphore.release();  // Always release
    }
}
```

**Benefits:**
- Prevents resource exhaustion
- Fair job scheduling (FIFO)
- Configurable capacity
- Graceful overload handling

### 2. Async Job Execution

```java
@Async("jobExecutorThreadPool")
public CompletableFuture<Void> executeAsync(JobDispatchEvent event) {
    // Job execution logic
    return CompletableFuture.completedFuture(null);
}
```

**Benefits:**
- Non-blocking dispatch endpoint
- Parallel job execution
- Better throughput
- Responsive service

### 3. Checkpoint Support

```java
// Save checkpoint every minute for long jobs
@Scheduled(fixedRate = 60000)
public void saveCheckpoints() {
    for (RunningJob job : runningJobs) {
        if (job.isCheckpointDue()) {
            saveCheckpoint(job.getJobId(), job.getCurrentState());
        }
    }
}
```

**Use Cases:**
- Long-running jobs (>5 minutes)
- Resume after failure
- Partial progress tracking
- Retry optimization

### 4. Error Handling & Retry

```java
try {
    executeJob(event);
    updateStatus(COMPLETED);
} catch (Exception e) {
    log.error("Job failed: {}", e.getMessage());
    updateStatus(FAILED, e.getMessage());
    
    if (event.getRetryCount() < event.getMaxRetries()) {
        // Reset to PENDING for retry
        scheduleRetry(event);
    }
}
```

## 📊 Service Dependencies

```
Worker Agent → Job Store Service (query jobs, update status, save executions)
Worker Agent → Execution Coordinator (heartbeat, registration)
Worker Agent ← Scheduler Worker (receives dispatch events)
```

### Required Services
1. **Job Store Service** (8081): Job data and status updates
2. **Execution Coordinator** (8085): Health monitoring

### Optional Services
- **Scheduler Worker** (8083): Job dispatch source

## 🧪 Testing Scenarios

### 1. Single Job Execution

```bash
# Submit and wait for job to be dispatched
curl -X POST http://localhost:8081/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "Test Job",
    "userId": 1,
    "frequency": "ONE_TIME",
    "executionTime": "2024-01-01T00:00:00Z",
    "payload": "Test data",
    "maxRetries": 3,
    "segment": 5
  }'

# Manually dispatch to worker agent (bypass scheduler)
curl -X POST http://localhost:8084/api/worker-agent/dispatch \
  -H "Content-Type: application/json" \
  -d '{
    "jobId": 1,
    "jobName": "Test Job",
    "userId": 1,
    "payload": "Test data",
    "executionTime": "2024-01-01T00:00:00Z",
    "maxRetries": 3,
    "retryCount": 0
  }'

# Check job status
curl http://localhost:8081/api/jobs/1
```

### 2. Concurrency Limit Test

```bash
# Dispatch 15 jobs simultaneously (limit is 10)
for i in {1..15}; do
  curl -X POST http://localhost:8084/api/worker-agent/dispatch \
    -H "Content-Type: application/json" \
    -d "{
      \"jobId\": $i,
      \"jobName\": \"Job $i\",
      \"userId\": 1,
      \"payload\": \"Data $i\",
      \"executionTime\": \"2024-01-01T00:00:00Z\",
      \"maxRetries\": 3,
      \"retryCount\": 0
    }" &
done

# Check worker status
curl http://localhost:8084/api/worker-agent/status

# Should show:
# - capacity: 10
# - currentLoad: 10 (max)
# - Some jobs may be rejected with 503
```

### 3. Failure & Retry Test

```bash
# Submit job with maxRetries = 3
# Simulate failure in job execution
# Check that job status becomes FAILED
# Check that job is reset to PENDING if retries available
curl http://localhost:8081/api/jobs/1
```

### 4. Checkpoint Test

```bash
# Submit long-running job (>5 minutes)
# Check executions table for checkpoint data
curl http://localhost:8081/api/executions?jobId=1

# Verify checkpoint_data is updated periodically
```

## 📈 Monitoring

### Key Metrics

1. **Throughput**
   - Jobs executed per minute
   - Average execution time
   - Queue wait time

2. **Capacity**
   - Current load vs. capacity
   - Semaphore utilization %
   - Rejected job count

3. **Success Rate**
   - Completed jobs
   - Failed jobs
   - Retry rate

4. **Performance**
   - Execution duration (p50, p95, p99)
   - Checkpoint overhead
   - Thread pool stats

### Health Check
```bash
# Check worker status
curl http://localhost:8084/api/worker-agent/status

# Check running jobs
curl http://localhost:8084/api/worker-agent/jobs/running
```

## 🐛 Troubleshooting

### Issue: Jobs Not Executing

**Possible Causes:**
1. Worker at capacity (all slots occupied)
2. Job Store Service unavailable
3. Invalid job data
4. Thread pool exhausted

**Solutions:**
```bash
# Check worker capacity
curl http://localhost:8084/api/worker-agent/status

# Check running jobs
curl http://localhost:8084/api/worker-agent/jobs/running

# Check job store connectivity
curl http://localhost:8081/actuator/health

# Restart worker if needed
```

### Issue: Jobs Stuck in RUNNING

**Possible Causes:**
1. Long-running job
2. Job execution exception not caught
3. Worker crashed mid-execution
4. Semaphore not released

**Solutions:**
```bash
# Check job execution history
curl http://localhost:8081/api/executions?jobId={id}

# Check worker logs for exceptions

# Execution Coordinator should detect and reassign
# after heartbeat timeout (60 seconds)
```

### Issue: High Failure Rate

**Possible Causes:**
1. Invalid job payload
2. External service unavailable
3. Resource constraints (memory, CPU)
4. Timeout too aggressive

**Solutions:**
```bash
# Check failed jobs
curl http://localhost:8081/api/jobs?status=FAILED

# Review error messages
curl http://localhost:8081/api/executions?jobId={id}

# Adjust concurrency limit if resource constrained
# Increase timeout if needed
```

## 📝 Important Notes

1. **Graceful Shutdown**: Wait for running jobs to complete
2. **Idempotency**: Job execution should be idempotent
3. **Timeout**: Configure appropriate execution timeout
4. **Memory**: Monitor heap usage with concurrent jobs
5. **Thread Pool**: Configure thread pool size appropriately
6. **Checkpoints**: Enable for jobs >5 minutes
7. **Heartbeat**: Must be sent regularly to avoid reassignment

## ⚠️ Critical Rules

**MANDATORY**:
- ✅ Respect concurrency limit (semaphore)
- ✅ Always update job status (RUNNING → COMPLETED/FAILED)
- ✅ Release semaphore in finally block
- ✅ Handle exceptions gracefully
- ✅ Send heartbeat every 30 seconds
- ✅ Validate job data before execution
- ❌ KHÔNG execute job với status != SCHEDULED
- ❌ KHÔNG block dispatch endpoint
- ❌ KHÔNG skip status updates

## 🔗 Related Documentation

- [System Design](../SYSTEM_DESIGN.md) - Overall architecture
- [Development Guide](../DEVELOPMENT_GUIDE.md) - Development standards
- [Diagrams](../DIAGRAMS.md) - Execution flow diagrams
- [Execution Coordinator README](../execution-coordinator/README.md) - Health monitoring details

---

**For the main system documentation, see [../README.md](../README.md)**
