package com.distributed.jobscheduler.worker.scheduler.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "dispatch_records")
@Data
public class DispatchRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "segment", nullable = false)
    private Integer segment;

    @Column(name = "dispatch_time", nullable = false)
    @CreationTimestamp
    private Instant dispatchTime;

    @Column(columnDefinition = "TEXT")
    private String payload;
}
