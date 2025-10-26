# Scheduler Coordinator

**Port:** 8082  
**Role:** Orchestrator & Leader Election

## 📖 Tổng Quan (Overview)

Scheduler Coordinator là service điều phối các scheduler worker nodes trong hệ thống. Service này thực hiện leader election để tránh single point of failure và quản lý việc phân phối segments cho các scheduler workers.

Scheduler Coordinator is the service that orchestrates scheduler worker nodes in the system. It performs leader election to avoid single points of failure and manages segment distribution to scheduler workers.

## 🎯 Trách Nhiệm (Responsibilities)

- **Leader Election**: Bầu chọn coordinator leader để điều phối hệ thống
- **Segment Assignment**: Phân phối 100 segments (0-99) cho scheduler workers
- **Health Monitoring**: Theo dõi sức khỏe của scheduler workers
- **Load Balancing**: Rebalance segments khi workers join/leave
- **Worker Registration**: Đăng ký và quản lý scheduler worker nodes

---

- **Leader Election**: Elect coordinator leader to orchestrate the system
- **Segment Assignment**: Distribute 100 segments (0-99) to scheduler workers
- **Health Monitoring**: Monitor health of scheduler workers
- **Load Balancing**: Rebalance segments when workers join/leave
- **Worker Registration**: Register and manage scheduler worker nodes

## 🏗️ Architecture Components

### 1. Leader Election
- Nhiều coordinator nodes có thể chạy đồng thời
- Chỉ 1 leader active tại một thời điểm
- Tự động failover khi leader failure
- Prevents single point of failure

### 2. Segment Management
- Tổng 100 segments (0-99)
- Mỗi segment được assign cho một scheduler worker
- Dynamic rebalancing khi topology thay đổi
- Đảm bảo không có segment bị duplicate assign

### 3. Heartbeat Monitoring
- Scheduler workers gửi heartbeat định kỳ
- Timeout detection (60 seconds)
- Tự động release segments từ unhealthy workers
- Trigger rebalancing khi cần thiết

## 🔌 API Endpoints

### Coordinator Management

#### Register Coordinator Node
```bash
POST /api/coordinator/register
Content-Type: application/json

{
  "coordinatorId": "coordinator-1",
  "host": "localhost",
  "port": 8082
}
```

#### Get Current Leader
```bash
GET /api/coordinator/leader
```

Response:
```json
{
  "success": true,
  "data": {
    "coordinatorId": "coordinator-1",
    "isLeader": true,
    "electedAt": "2024-12-31T10:00:00Z"
  }
}
```

#### Send Coordinator Heartbeat
```bash
POST /api/coordinator/heartbeat?coordinatorId=coordinator-1
```

### Scheduler Worker Management

#### Register Scheduler Worker
```bash
POST /api/scheduler-workers/register?workerId=worker-1
```

Response:
```json
{
  "success": true,
  "data": {
    "workerId": "worker-1",
    "status": "ONLINE",
    "assignedSegments": []
  }
}
```

#### Send Worker Heartbeat
```bash
POST /api/scheduler-workers/heartbeat?workerId=worker-1
```

#### List Active Scheduler Workers
```bash
GET /api/scheduler-workers
```

### Segment Management

#### Assign Segments to Worker
```bash
POST /api/segments/assign?workerId=worker-1&desiredSegments=10
```

Tự động assign 10 segments available cho worker.

#### Release Worker Segments
```bash
DELETE /api/segments/release?workerId=worker-1
```

Release tất cả segments của worker khi offline.

#### Get Worker Segments
```bash
GET /api/segments?workerId=worker-1
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

## 🗄️ Database Entities

### CoordinatorEntity
- `coordinatorId`: Unique identifier
- `isLeader`: Leader status flag
- `lastHeartbeatTime`: Last heartbeat timestamp
- `electedAt`: Leader election timestamp

### SchedulerWorkerEntity
- `workerId`: Unique identifier
- `status`: ONLINE, OFFLINE, UNHEALTHY
- `lastHeartbeatTime`: Health tracking
- `assignedSegments`: List of segment IDs

### SegmentEntity
- `segmentId`: 0-99
- `workerId`: Assigned worker (nullable)
- `assignedAt`: Assignment timestamp
- `status`: AVAILABLE, ASSIGNED

## 🔄 Leader Election Algorithm

```
1. Coordinator starts up
2. Register itself in database
3. Check if there's an active leader
4. If no leader or leader timeout:
   - Try to claim leadership
   - Set isLeader = true
   - Update electedAt timestamp
5. Send periodic heartbeats
6. Other coordinators monitor leader heartbeat
7. If leader fails (no heartbeat for 60s):
   - Eligible coordinators attempt to become leader
   - First to update wins (optimistic locking)
```

## 📊 Segment Assignment Algorithm

```
1. Scheduler Worker registers with coordinator
2. Requests N segments (desiredSegments)
3. Coordinator (Leader only) executes:
   a. Query available segments (not assigned)
   b. Assign first N available segments to worker
   c. Update segment status to ASSIGNED
   d. Notify worker of assigned segments
4. Worker starts scanning assigned segments
```

## 🚀 How to Run

### Prerequisites
- Java 21
- Maven 3.6+

### Build
```bash
cd scheduler-coordinator
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Service sẽ chạy trên **http://localhost:8082**

### Run Multiple Coordinators (High Availability)
```bash
# Terminal 1 - Coordinator 1 (default port 8082)
mvn spring-boot:run

# Terminal 2 - Coordinator 2 (port 8092)
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8092

# Terminal 3 - Coordinator 3 (port 8093)
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8093
```

## 🔑 Key Features

### 1. High Availability
- Nhiều coordinator instances có thể chạy
- Leader election tự động
- No single point of failure
- Automatic failover

### 2. Dynamic Scaling
- Scheduler workers có thể join/leave bất cứ lúc nào
- Automatic segment rebalancing
- Zero downtime operations

### 3. Health Monitoring
- Periodic heartbeat checks (every 30 seconds)
- Timeout detection (60 seconds)
- Automatic cleanup of failed workers

### 4. Segment Distribution
- Fair distribution của segments
- Prevents overloading single worker
- Configurable segment allocation

## 📈 Monitoring and Operations

### Check Leader Status
```bash
curl http://localhost:8082/api/coordinator/leader
```

### View Active Workers
```bash
curl http://localhost:8082/api/scheduler-workers
```

### Monitor Segment Distribution
```bash
# Check segments for specific worker
curl http://localhost:8082/api/segments?workerId=worker-1
```

## 🧪 Testing Scenarios

### 1. Leader Election Test
```bash
# Start multiple coordinators
# Check which one becomes leader
curl http://localhost:8082/api/coordinator/leader
curl http://localhost:8092/api/coordinator/leader

# Stop leader coordinator
# Check automatic failover to backup
```

### 2. Segment Assignment Test
```bash
# Register worker
curl -X POST "http://localhost:8082/api/scheduler-workers/register?workerId=worker-1"

# Request segments
curl -X POST "http://localhost:8082/api/segments/assign?workerId=worker-1&desiredSegments=10"

# Verify assignment
curl "http://localhost:8082/api/segments?workerId=worker-1"
```

### 3. Worker Failover Test
```bash
# Register worker and assign segments
curl -X POST "http://localhost:8082/api/scheduler-workers/register?workerId=worker-1"
curl -X POST "http://localhost:8082/api/segments/assign?workerId=worker-1&desiredSegments=10"

# Stop sending heartbeats (simulate failure)
# Wait 60 seconds

# Check segments are released
curl "http://localhost:8082/api/segments?workerId=worker-1"
```

## ⚙️ Configuration

```yaml
server:
  port: 8082

spring:
  application:
    name: scheduler-coordinator
  
  datasource:
    url: jdbc:h2:mem:coordinator
    driver-class-name: org.h2.Driver

scheduler:
  coordinator:
    heartbeat-interval: 30000  # 30 seconds
    heartbeat-timeout: 60000   # 60 seconds
    leader-election-interval: 10000  # 10 seconds
```

## 📊 Service Dependencies

```
Scheduler Coordinator ← Scheduler Worker (heartbeat, registration)
Scheduler Coordinator → Job Store Service (worker data - optional)
```

## 🔗 Related Services

- **Scheduler Worker** (Port 8083): Consumes segment assignments
- **Job Store Service** (Port 8081): Persistent storage (optional)

## 📝 Important Notes

1. **Only Leader Acts**: Chỉ leader coordinator thực hiện segment assignment
2. **Idempotency**: Worker registration và heartbeat là idempotent
3. **Graceful Shutdown**: Workers nên release segments trước khi shutdown
4. **Database**: Shared database hoặc distributed consensus cần thiết cho leader election
5. **Timeout Configuration**: Heartbeat timeout nên >= 2x heartbeat interval

## ⚠️ Critical Rules

**MANDATORY**:
- ✅ Chỉ leader coordinator được assign segments
- ✅ Heartbeat monitoring phải chạy liên tục
- ✅ Segment assignment phải atomic
- ✅ Worker registration phải idempotent
- ⚠️ Không được skip leader election checks

## 🔗 Related Documentation

- [System Design](../SYSTEM_DESIGN.md) - Overall architecture
- [Development Guide](../DEVELOPMENT_GUIDE.md) - Development standards
- [Diagrams](../DIAGRAMS.md) - Leader election flow diagrams

---

**For the main system documentation, see [../README.md](../README.md)**
