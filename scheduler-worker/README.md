# Scheduler Worker

**Port:** 8083  
**Role:** Job Schedule Scanner & Dispatcher

## 📖 Tổng Quan (Overview)

Scheduler Worker là service quét job schedules định kỳ và dispatch jobs tới Worker Agents để thực thi. Service này hoạt động dựa trên segment assignment từ Scheduler Coordinator, đảm bảo không có job nào bị dispatch duplicate.

Scheduler Worker is the service that periodically scans job schedules and dispatches jobs to Worker Agents for execution. It operates based on segment assignments from Scheduler Coordinator, ensuring no job is dispatched duplicate.

## 🎯 Trách Nhiệm (Responsibilities)

- **Schedule Scanning**: Quét job_schedules table theo assigned segments
- **Job Dispatching**: Gửi due jobs tới Worker Agent để thực thi
- **Status Management**: Cập nhật job status từ PENDING → SCHEDULED
- **Coordinator Registration**: Đăng ký với Scheduler Coordinator
- **Heartbeat Reporting**: Gửi heartbeat định kỳ để báo health status
- **Segment Management**: Nhận và quản lý assigned segments

---

- **Schedule Scanning**: Scan job_schedules table by assigned segments
- **Job Dispatching**: Send due jobs to Worker Agent for execution
- **Status Management**: Update job status from PENDING → SCHEDULED
- **Coordinator Registration**: Register with Scheduler Coordinator
- **Heartbeat Reporting**: Send periodic heartbeat for health status
- **Segment Management**: Receive and manage assigned segments

## 🏗️ Architecture Components

### 1. Schedule Scanner
```java
@Scheduled(fixedRate = 60000) // Every 60 seconds
public void scanDueJobs() {
    // 1. Get assigned segments from coordinator
    // 2. Query due jobs for those segments
    // 3. For each due job:
    //    - Update status to SCHEDULED
    //    - Dispatch to Worker Agent
    //    - Update next_run_time (for recurring jobs)
}
```

### 2. Job Dispatcher
- Gửi job dispatch events tới Worker Agent
- Retry mechanism nếu dispatch fails
- Idempotent dispatching

### 3. Segment Manager
- Lưu trữ assigned segments từ coordinator
- Dynamic update khi segments thay đổi
- Validate segments trước khi scanning

### 4. Heartbeat Reporter
```java
@Scheduled(fixedRate = 30000) // Every 30 seconds
public void sendHeartbeat() {
    // Send heartbeat to coordinator
    // Report current load and status
}
```

## 🔌 API Endpoints

### Segment Management

#### Update Assigned Segments
```bash
POST /api/scheduler-worker/segments
Content-Type: application/json

{
  "segments": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
}
```

Endpoint này được gọi bởi Scheduler Coordinator khi assign segments.

#### Get Assigned Segments
```bash
GET /api/scheduler-worker/segments
```

Response:
```json
{
  "success": true,
  "data": {
    "workerId": "worker-1",
    "segments": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
  }
}
```

### Health Check

#### Get Worker Status
```bash
GET /api/scheduler-worker/status
```

Response:
```json
{
  "success": true,
  "data": {
    "workerId": "worker-1",
    "status": "ONLINE",
    "assignedSegmentCount": 10,
    "lastScanTime": "2024-12-31T10:00:00Z",
    "jobsDispatchedCount": 150
  }
}
```

## 🔄 Job Scanning Flow

```
1. Timer triggers (every 60 seconds)
2. Get assigned segments from local state
3. Query Job Store Service:
   GET /api/job-schedules/due?asOf={now}&segments={assigned}
4. For each due job:
   a. Update job status to SCHEDULED
      PUT /api/jobs/{id}/status?status=SCHEDULED
   b. Create dispatch event
   c. Send to Worker Agent
      POST /api/worker-agent/dispatch
      {
        "jobId": 123,
        "jobName": "Example Job",
        "payload": "...",
        "executionTime": "..."
      }
   d. Update next_run_time if recurring job
5. Log scan statistics
```

## 🚀 How to Run

### Prerequisites
- Java 21
- Maven 3.6+
- Job Store Service running (port 8081)
- Scheduler Coordinator running (port 8082)
- Worker Agent running (port 8084)

### Build
```bash
cd scheduler-worker
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Service sẽ chạy trên **http://localhost:8083**

### Run Multiple Workers (Horizontal Scaling)
```bash
# Terminal 1 - Worker 1 (default port 8083)
mvn spring-boot:run

# Terminal 2 - Worker 2 (port 8183)
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8183 --scheduler.worker.worker-id=worker-2"

# Terminal 3 - Worker 3 (port 8283)
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=8283 --scheduler.worker.worker-id=worker-3"
```

## ⚙️ Configuration

```yaml
server:
  port: 8083

spring:
  application:
    name: scheduler-worker

scheduler:
  worker:
    worker-id: worker-1              # Unique worker identifier
    assigned-segments: []            # Initially empty, assigned by coordinator
    coordinator-url: http://localhost:8082  # Scheduler Coordinator URL
    job-store-url: http://localhost:8081    # Job Store Service URL
    scan-interval: 60000             # Scan interval in ms (60 seconds)
    heartbeat-interval: 30000        # Heartbeat interval in ms (30 seconds)
```

## 🔑 Key Features

### 1. Segment-Based Scanning
```
- Total 100 segments (0-99)
- Each worker handles specific segments
- Prevents duplicate job dispatching
- Enables horizontal scaling
- Example: Worker-1 handles segments [0-9]
          Worker-2 handles segments [10-19]
```

### 2. Idempotent Dispatching
```
- Check job status before dispatching
- Only dispatch if status = PENDING
- Record dispatch time
- Prevent duplicate execution
```

### 3. Periodic Scanning
```
- Fixed rate: 60 seconds
- Configurable interval
- Scan only assigned segments
- Query jobs where next_run_time <= now
```

### 4. High Availability
```
- Multiple workers can run simultaneously
- Each handles different segments
- Automatic load balancing via coordinator
- Worker failure doesn't lose jobs
```

## 📊 Service Dependencies

```
Scheduler Worker → Scheduler Coordinator (registration, heartbeat, segments)
Scheduler Worker → Job Store Service (query schedules, update status)
Scheduler Worker → Worker Agent (dispatch jobs)
```

### Required Services
1. **Scheduler Coordinator** (8082): Segment assignment
2. **Job Store Service** (8081): Job data storage
3. **Worker Agent** (8084): Job execution

## 🧪 Testing Scenarios

### 1. Basic Workflow Test

```bash
# Step 1: Submit a job (due now)
curl -X POST http://localhost:8081/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "Test Job",
    "userId": 1,
    "frequency": "ONE_TIME",
    "executionTime": "2024-12-31T10:00:00Z",
    "payload": "Test payload",
    "maxRetries": 3,
    "segment": 5
  }'

# Step 2: Register worker with coordinator
curl -X POST "http://localhost:8082/api/scheduler-workers/register?workerId=worker-1"

# Step 3: Request segment assignment (must include segment 5)
curl -X POST "http://localhost:8082/api/segments/assign?workerId=worker-1&desiredSegments=10"

# Step 4: Wait for scan cycle (60 seconds)
# Or manually trigger scan (if debug endpoint available)

# Step 5: Check job status (should be SCHEDULED)
curl http://localhost:8081/api/jobs/1
```

### 2. Multiple Workers Test

```bash
# Register Worker 1
curl -X POST "http://localhost:8082/api/scheduler-workers/register?workerId=worker-1"
curl -X POST "http://localhost:8082/api/segments/assign?workerId=worker-1&desiredSegments=50"

# Register Worker 2
curl -X POST "http://localhost:8082/api/scheduler-workers/register?workerId=worker-2"
curl -X POST "http://localhost:8082/api/segments/assign?workerId=worker-2&desiredSegments=50"

# Submit jobs with different segments
# Worker 1 handles segments 0-49
# Worker 2 handles segments 50-99
```

### 3. Recurring Job Test

```bash
# Submit daily recurring job
curl -X POST http://localhost:8081/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "Daily Report",
    "userId": 1,
    "frequency": "DAILY",
    "executionTime": "2024-12-31T09:00:00Z",
    "payload": "Generate report",
    "maxRetries": 3,
    "segment": 10
  }'

# After first execution:
# - Job status resets to PENDING
# - next_run_time updated to next day
# - Scanner picks it up again next day
```

## 📈 Monitoring

### Key Metrics to Monitor

1. **Scan Performance**
   - Scan duration
   - Jobs scanned per cycle
   - Jobs dispatched per cycle

2. **Dispatch Success Rate**
   - Successful dispatches
   - Failed dispatches
   - Retry attempts

3. **Segment Distribution**
   - Assigned segment count
   - Load per segment
   - Rebalancing events

4. **Worker Health**
   - Heartbeat success rate
   - Last heartbeat time
   - Connection errors

### Health Check
```bash
curl http://localhost:8083/api/scheduler-worker/status
```

## 🐛 Troubleshooting

### Issue: Jobs Not Being Dispatched

**Possible Causes:**
1. Worker không có assigned segments
2. Job segment không match assigned segments
3. Job executionTime chưa đến
4. Worker Agent không available
5. Job status không phải PENDING

**Solutions:**
```bash
# Check assigned segments
curl http://localhost:8083/api/scheduler-worker/segments

# Check job details
curl http://localhost:8081/api/jobs/{id}

# Check Worker Agent status
curl http://localhost:8084/api/worker-agent/status
```

### Issue: Duplicate Job Dispatching

**Cause:** Multiple workers assigned same segment

**Solution:**
```bash
# Check segment distribution
curl http://localhost:8082/api/segments

# Verify no overlap in worker segments
```

### Issue: Worker Not Receiving Segments

**Possible Causes:**
1. Coordinator không phải leader
2. Worker registration failed
3. No available segments

**Solutions:**
```bash
# Check coordinator leader
curl http://localhost:8082/api/coordinator/leader

# Re-register worker
curl -X POST "http://localhost:8082/api/scheduler-workers/register?workerId=worker-1"

# Request segments again
curl -X POST "http://localhost:8082/api/segments/assign?workerId=worker-1&desiredSegments=10"
```

## 📝 Important Notes

1. **Segment Assignment**: Worker phải có assigned segments trước khi scan
2. **Time Synchronization**: System clocks phải sync để next_run_time chính xác
3. **Database Locking**: Job status update phải atomic để tránh race conditions
4. **Graceful Shutdown**: Release segments trước khi shutdown
5. **Retry Logic**: Implement exponential backoff cho failed dispatches

## ⚠️ Critical Rules

**MANDATORY**:
- ✅ Chỉ scan jobs của assigned segments
- ✅ Update job status trước khi dispatch
- ✅ Send heartbeat every 30 seconds
- ✅ Handle coordinator failures gracefully
- ❌ KHÔNG dispatch job với status != PENDING
- ❌ KHÔNG scan segments không được assign

## 🔗 Related Documentation

- [System Design](../SYSTEM_DESIGN.md) - Overall architecture
- [Development Guide](../DEVELOPMENT_GUIDE.md) - Development standards
- [Diagrams](../DIAGRAMS.md) - Scheduling flow diagrams
- [Scheduler Coordinator README](../scheduler-coordinator/README.md) - Segment assignment details

---

**For the main system documentation, see [../README.md](../README.md)**
