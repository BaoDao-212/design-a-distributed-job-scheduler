package com.distributed.jobscheduler.worker.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "scheduler.worker")
@Data
public class WorkerConfig {
    private String workerId;
    private List<Integer> assignedSegments;
    private String coordinatorUrl;
    private String jobStoreUrl;
}
