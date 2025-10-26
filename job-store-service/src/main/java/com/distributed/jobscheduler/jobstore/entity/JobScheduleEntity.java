package com.distributed.jobscheduler.jobstore.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "job_schedules")
@Data
public class JobScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "next_run_time", nullable = false)
    private Instant nextRunTime;

    @Column(name = "last_run_time")
    private Instant lastRunTime;

    @Column(nullable = false)
    private Integer segment;
}
