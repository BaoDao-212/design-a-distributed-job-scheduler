package com.distributed.jobscheduler.jobstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkerRegistrationRequest {

    @NotBlank(message = "Worker ID must be provided")
    private String workerId;

    @NotNull(message = "Segment must be provided")
    private Integer segment;

    private Integer capacity;
}
