package com.distributed.jobscheduler.execution.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "reassignment_events")
@Data
public class ReassignmentEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "from_worker_id", nullable = false)
    private String fromWorkerId;

    @Column(name = "to_worker_id", nullable = false)
    private String toWorkerId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "checkpoint_data", columnDefinition = "TEXT")
    private String checkpointData;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
