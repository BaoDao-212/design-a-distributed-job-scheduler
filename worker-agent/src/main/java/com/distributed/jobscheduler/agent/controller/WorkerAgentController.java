package com.distributed.jobscheduler.agent.controller;

import com.distributed.jobscheduler.agent.service.JobExecutionService;
import com.distributed.jobscheduler.common.dto.JobDispatchEvent;
import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worker-agent")
@RequiredArgsConstructor
public class WorkerAgentController {

    private final JobExecutionService jobExecutionService;

    @PostMapping("/dispatch")
    public ResponseData<String> dispatchJob(@Valid @RequestBody JobDispatchEvent event) {
        jobExecutionService.executeJob(event);
        return ResponseUtils.success("Job execution triggered");
    }
}
