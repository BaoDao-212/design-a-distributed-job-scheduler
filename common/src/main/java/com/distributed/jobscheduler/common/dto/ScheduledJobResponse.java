package com.distributed.jobscheduler.common.dto;

import com.distributed.jobscheduler.common.enums.JobFrequency;
import com.distributed.jobscheduler.common.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobResponse {
    private Long jobId;
    private Long scheduleId;
    private String jobName;
    private JobFrequency frequency;
    private JobStatus status;
    private Instant nextRunTime;
    private Integer segment;
    private String payload;
}
