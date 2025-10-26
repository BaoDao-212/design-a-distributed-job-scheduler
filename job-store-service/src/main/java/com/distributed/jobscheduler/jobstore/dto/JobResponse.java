package com.distributed.jobscheduler.jobstore.dto;

import com.distributed.jobscheduler.common.enums.JobFrequency;
import com.distributed.jobscheduler.common.enums.JobStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class JobResponse {
    private Long id;
    private String jobName;
    private Long userId;
    private JobFrequency frequency;
    private String payload;
    private Instant executionTime;
    private Integer retryCount;
    private Integer maxRetries;
    private JobStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
