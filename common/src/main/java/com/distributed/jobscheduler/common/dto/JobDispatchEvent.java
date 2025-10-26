package com.distributed.jobscheduler.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDispatchEvent {
    private Long jobId;
    private String jobName;
    private String payload;
    private Instant scheduledTime;
    private Integer maxRetries;
    private Integer currentRetryCount;
}
