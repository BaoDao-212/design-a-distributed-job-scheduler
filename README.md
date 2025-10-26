# Distributed Job Scheduler - Spring Boot Microservices

A scalable distributed job scheduling system built with Spring Boot microservices architecture. The system can handle millions of tasks, ensure high availability, and prevent single points of failure through segment-based partitioning and leader election.

## 🏗️ Architecture Overview

This project implements a distributed job scheduler consisting of **5 microservices**:

### Services

| Service | Port | Description |
|---------|------|-------------|
| **job-store-service** | 8081 | Centralized persistence layer for jobs, schedules, executions, and workers |
| **scheduler-coordinator** | 8082 | Orchestrates scheduler workers with leader election and segment assignment |
| **scheduler-worker** | 8083 | Scans job schedules by segments and dispatches jobs |
| **worker-agent** | 8084 | Executes jobs with concurrency control |
| **execution-coordinator** | 8085 | Monitors worker health and handles job reassignment on failures |

## 🚀 Quick Start

### Prerequisites
- Java 21
- Maven 3.6+

### Build All Services
```bash
cd /home/engine/project
mvn clean install
```

### Run Services

Each service can be run independently:

```bash
# Terminal 1 - Job Store Service
cd job-store-service
mvn spring-boot:run

# Terminal 2 - Scheduler Coordinator
cd scheduler-coordinator
mvn spring-boot:run

# Terminal 3 - Scheduler Worker
cd scheduler-worker
mvn spring-boot:run

# Terminal 4 - Worker Agent
cd worker-agent
mvn spring-boot:run

# Terminal 5 - Execution Coordinator
cd execution-coordinator
mvn spring-boot:run
```

## 📖 Documentation

Comprehensive documentation is available:

### 📊 [DIAGRAMS.md](./DIAGRAMS.md)
**Architecture and flow diagrams**
- High-level service overview
- Sequence diagrams (submission, dispatch, execution)
- Leader election & health monitoring flows
- Segment partitioning and scaling strategy
- Database schema relationships
- 12 comprehensive Mermaid diagrams

### 🔧 [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md)
**Complete system design and API reference**
- Detailed service descriptions
- API endpoints for all services
- Database schema
- Design patterns and best practices

### 📋 [ARCHITECTURE.md](./ARCHITECTURE.md)
**System architecture principles**
- Project structure and technology stack
- Component relationships and data flow
- Architectural decisions

### 🚀 [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md)
**Development workflow and standards**
- Setup instructions and prerequisites
- Step-by-step component creation
- Code templates and examples
- **CRITICAL RULES and restrictions**

### 🔄 [API_PATTERNS.md](./API_PATTERNS.md)
**API design patterns**
- Standardized response structures
- Controller implementation patterns
- Error handling and validation

### ⚙️ [CONFIGURATION.md](./CONFIGURATION.md)
**Configuration and deployment**
- Application configuration files
- Database and security settings
- Environment-specific configurations

## 🔑 Key Features

### Scalability
- **Segment-based partitioning** (100 segments: 0-99) for horizontal scaling
- Multiple coordinator nodes with leader election
- Multiple scheduler workers and worker agents
- Each handles specific job segments

### High Availability
- **No single point of failure** through leader election
- Worker health monitoring with heartbeat mechanism
- Automatic job reassignment on worker failure
- Fault-tolerant job execution

### Consistency
- Idempotent job dispatching via dispatch records
- Segment-based job ownership prevents duplication
- Status tracking for each execution
- Checkpoint support for retries

### Performance
- Configurable concurrency per worker
- Periodic scanning (60s for scheduling, 30s for health monitoring)
- Direct REST API communication
- H2 in-memory database for development

## 🧪 Testing the System

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

## 🛠️ Technology Stack

- **Spring Boot 3.3.4** - Application framework
- **Java 21** - Programming language
- **H2 Database** - In-memory database (development)
- **PostgreSQL** - Recommended for production
- **Maven** - Build tool
- **Lombok** - Code generation
- **MapStruct** - Object mapping

## 📊 Database Schema

### Core Tables
- **jobs** - Job metadata (name, frequency, payload, status, retry count)
- **job_schedules** - Scheduling data with next_run_time and segment
- **job_executions** - Execution history with checkpoint data
- **workers** - Worker registration and capacity
- **worker_heartbeats** - Worker health tracking

### Design Principles
- **No JPA relationships** - Only ID references for simplicity
- Service layer coordination for related data
- Flat entity model for easier maintenance

## 🔄 System Flow

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

## 🎯 Future Enhancements

- Kafka integration for job dispatch events
- Job prioritization with priority queues
- Job dependencies and workflow support
- Rate limiting at client and queue levels
- Prometheus metrics and distributed tracing
- Full checkpoint/resume implementation
- Backpressure handling
- Dead letter queue for failed jobs

## 📝 License

This project is part of a distributed systems learning exercise.

## 🤝 Contributing

Please read the development guide in [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md) before contributing.

## ⚠️ Critical Rules

**MANDATORY READING**: All developers must follow:
- **NO JPA Relationships** (@OneToMany, @ManyToOne, @OneToOne, @ManyToMany)
- Entity design restrictions
- Mandatory patterns and conventions

See [DEVELOPMENT_GUIDE.md](./DEVELOPMENT_GUIDE.md) for complete details.

---

**For visual architecture diagrams, see [DIAGRAMS.md](./DIAGRAMS.md)**  
**For detailed system design, see [SYSTEM_DESIGN.md](./SYSTEM_DESIGN.md)**
