package com.distributed.jobscheduler.jobstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "worker_heartbeats")
@Data
public class WorkerHeartbeatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", unique = true, nullable = false, length = 100)
    private String workerId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @UpdateTimestamp
    @Column(name = "last_heartbeat", nullable = false)
    private Instant lastHeartbeat;

    @Column(name = "available_capacity")
    private Integer availableCapacity;
}
