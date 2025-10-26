package com.distributed.jobscheduler.jobstore.entity;

import com.distributed.jobscheduler.common.enums.WorkerStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "workers")
@Data
public class WorkerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", unique = true, nullable = false, length = 100)
    private String workerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerStatus status = WorkerStatus.UNASSIGNED;

    @Column(name = "segment", nullable = false)
    private Integer segment;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "current_load")
    private Integer currentLoad;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
