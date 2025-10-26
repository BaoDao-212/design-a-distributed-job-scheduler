package com.distributed.jobscheduler.jobstore.dto;

import com.distributed.jobscheduler.common.enums.JobFrequency;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class JobSubmissionRequest {

    @NotBlank(message = "Job name is required")
    private String jobName;

    @NotNull(message = "User ID must be provided")
    private Long userId;

    @NotNull(message = "Frequency must be specified")
    private JobFrequency frequency;

    @NotNull(message = "Execution time must be set")
    @FutureOrPresent(message = "Execution time cannot be in the past")
    private Instant executionTime;

    @NotNull(message = "Payload is required")
    private String payload;

    private Integer maxRetries = 3;

    private Integer segment = 0;
}
