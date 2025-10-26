package com.distributed.jobscheduler.execution.entity;

import com.distributed.jobscheduler.common.enums.WorkerStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "execution_workers")
@Data
public class ExecutionWorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false, unique = true, length = 100)
    private String workerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerStatus status = WorkerStatus.ONLINE;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "current_load")
    private Integer currentLoad;

    @Column(name = "worker_host")
    private String workerHost;

    @Column(name = "worker_port")
    private Integer workerPort;

    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
