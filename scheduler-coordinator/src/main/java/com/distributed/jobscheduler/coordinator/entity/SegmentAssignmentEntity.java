package com.distributed.jobscheduler.coordinator.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "segment_assignments")
@Data
public class SegmentAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false, length = 100)
    private String workerId;

    @Column(name = "segment", nullable = false)
    private Integer segment;

    @UpdateTimestamp
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
}
