# Job Store Service

**Port:** 8081  
**Role:** Centralized Persistence Layer

## 📖 Tổng Quan (Overview)

Job Store Service là tầng persistence trung tâm của hệ thống Distributed Job Scheduler. Service này chịu trách nhiệm lưu trữ và quản lý tất cả dữ liệu liên quan đến jobs, schedules, executions, và workers.

Job Store Service is the central persistence layer of the Distributed Job Scheduler system. This service is responsible for storing and managing all data related to jobs, schedules, executions, and workers.

## 🎯 Trách Nhiệm (Responsibilities)

- **Quản lý Jobs**: Tạo, cập nhật, xóa và truy vấn thông tin jobs
- **Quản lý Schedules**: Lưu trữ lịch trình thực thi với next_run_time và segment
- **Theo dõi Executions**: Ghi lại lịch sử thực thi jobs với checkpoint data
- **Đăng ký Workers**: Lưu trữ thông tin workers và heartbeats
- **API Gateway**: Cung cấp REST API cho tất cả các services khác

---

- **Job Management**: Create, update, delete and query job information
- **Schedule Management**: Store execution schedules with next_run_time and segment
- **Execution Tracking**: Record job execution history with checkpoint data
- **Worker Registration**: Store worker information and heartbeats
- **API Gateway**: Provide REST API for all other services

## 🗃️ Entities

### 1. JobEntity
Lưu trữ metadata của job:
- `jobName`: Tên job
- `userId`: ID người dùng tạo job
- `frequency`: Tần suất thực thi (ONE_TIME, DAILY, WEEKLY, MONTHLY)
- `payload`: Dữ liệu job (TEXT/JSON)
- `executionTime`: Thời gian thực thi
- `retryCount`, `maxRetries`: Quản lý retry
- `status`: PENDING, SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED
- `segment`: Segment phân vùng (0-99)

### 2. JobScheduleEntity
Lưu trữ thông tin lịch trình:
- `jobId`: Reference đến job
- `nextRunTime`: Thời gian chạy tiếp theo
- `lastRunTime`: Thời gian chạy cuối
- `segment`: Segment để phân vùng (0-99)

### 3. JobExecutionEntity
Theo dõi lịch sử thực thi:
- `jobId`: Reference đến job
- `workerId`: Worker thực thi
- `startTime`, `endTime`: Thời gian thực thi
- `status`: Trạng thái execution
- `errorMessage`: Thông báo lỗi (nếu có)
- `checkpointData`: Dữ liệu checkpoint để resume

### 4. WorkerEntity
Thông tin worker:
- `workerId`: Unique identifier
- `status`: ONLINE, OFFLINE, UNHEALTHY
- `segment`: Segment được assign
- `capacity`: Công suất tối đa
- `currentLoad`: Tải hiện tại

### 5. WorkerHeartbeatEntity
Theo dõi sức khỏe worker:
- `workerId`: Reference đến worker
- `lastHeartbeatTime`: Thời gian heartbeat cuối
- `status`: Trạng thái health

## 🔌 API Endpoints

### Jobs API

#### Submit New Job
```bash
POST /api/jobs
Content-Type: application/json

{
  "jobName": "Example Job",
  "userId": 1,
  "frequency": "ONE_TIME",
  "executionTime": "2024-12-31T10:00:00Z",
  "payload": "Job payload data",
  "maxRetries": 3,
  "segment": 5
}
```

#### Get Job by ID
```bash
GET /api/jobs/{id}
```

#### Get Jobs by Status
```bash
GET /api/jobs?status=PENDING
GET /api/jobs?status=RUNNING
```

#### Update Job Status
```bash
PUT /api/jobs/{id}/status?status=RUNNING
```

#### Cancel Job
```bash
DELETE /api/jobs/{id}
```

### Job Schedules API

#### Get Due Schedules
```bash
GET /api/job-schedules/due?asOf=2024-12-31T10:00:00Z&segments=0,1,2,3,4
```

### Workers API

#### Register Worker
```bash
POST /api/workers
Content-Type: application/json

{
  "workerId": "worker-1",
  "capacity": 10,
  "segment": 5
}
```

#### List All Workers
```bash
GET /api/workers
```

#### Update Worker Heartbeat
```bash
POST /api/workers/{workerId}/heartbeat
```

## 🗄️ Database Configuration

Service sử dụng H2 in-memory database cho development:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:jobstore;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: sa
  
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

**Production**: Khuyến nghị sử dụng PostgreSQL database.

## 🚀 How to Run

### Prerequisites
- Java 21
- Maven 3.6+

### Build
```bash
cd job-store-service
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

Service sẽ chạy trên **http://localhost:8081**

## 🔑 Key Design Principles

### 1. No JPA Relationships
- **Không sử dụng** @OneToMany, @ManyToOne, @OneToOne, @ManyToMany
- Chỉ lưu trữ ID references (jobId, workerId, userId)
- Service layer coordination để fetch related data

### 2. Response Pattern
```java
// Sử dụng ResponseData<T> wrapper
return ResponseUtils.success(data);

// Trả về "OK" cho void operations (không dùng Void)
return ResponseUtils.success("OK");
```

### 3. Segment-Based Partitioning
- 100 segments (0-99)
- Mỗi job được assign vào một segment
- Cho phép horizontal scaling
- Ngăn chặn duplicate dispatching

## 📊 Service Dependencies

Job Store Service **KHÔNG phụ thuộc** vào service nào khác.  
Tất cả services khác gọi vào Job Store Service:

```
Scheduler Worker → Job Store Service
Worker Agent → Job Store Service
Scheduler Coordinator → Job Store Service (gián tiếp)
Execution Coordinator → Job Store Service (gián tiếp)
```

## 🧪 Testing the Service

### 1. Check Service Health
```bash
curl http://localhost:8081/actuator/health
```

### 2. Submit a Test Job
```bash
curl -X POST http://localhost:8081/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobName": "Test Job",
    "userId": 1,
    "frequency": "ONE_TIME",
    "executionTime": "2024-12-31T10:00:00Z",
    "payload": "Hello World",
    "maxRetries": 3,
    "segment": 5
  }'
```

### 3. Query Job Status
```bash
curl http://localhost:8081/api/jobs/1
```

### 4. Get Pending Jobs
```bash
curl http://localhost:8081/api/jobs?status=PENDING
```

## 📝 Important Notes

1. **Central Data Store**: Tất cả data được lưu trữ tại đây
2. **Stateless**: Service này là stateless, có thể scale horizontally
3. **Database**: Sử dụng connection pooling để tối ưu performance
4. **Timestamps**: Tất cả entities có created_at và updated_at tự động
5. **Validation**: Input validation được thực hiện ở controller layer

## 🔗 Related Documentation

- [System Design](../SYSTEM_DESIGN.md) - Chi tiết về system architecture
- [Development Guide](../DEVELOPMENT_GUIDE.md) - Hướng dẫn phát triển
- [API Patterns](../API_PATTERNS.md) - Chuẩn API design
- [Database Diagrams](../DIAGRAMS.md) - Database schema visualization

## ⚠️ Critical Rules

**MANDATORY**: 
- ❌ KHÔNG sử dụng JPA relationships
- ✅ Chỉ dùng ID references
- ✅ Service layer coordination cho related data
- ✅ ResponseData<T> wrapper cho tất cả responses

---

**For the main system documentation, see [../README.md](../README.md)**
