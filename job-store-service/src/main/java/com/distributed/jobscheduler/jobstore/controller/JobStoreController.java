package com.distributed.jobscheduler.jobstore.controller;

import com.distributed.jobscheduler.common.enums.JobStatus;
import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import com.distributed.jobscheduler.jobstore.dto.JobResponse;
import com.distributed.jobscheduler.jobstore.dto.JobSubmissionRequest;
import com.distributed.jobscheduler.jobstore.service.JobStoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobStoreController {

    private final JobStoreService jobStoreService;

    @PostMapping
    public ResponseData<JobResponse> submitJob(@Valid @RequestBody JobSubmissionRequest request) {
        JobResponse response = jobStoreService.submitJob(request);
        return ResponseUtils.success(response);
    }

    @GetMapping("/{id}")
    public ResponseData<JobResponse> getJobById(@PathVariable Long id) {
        JobResponse response = jobStoreService.getJobById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
        return ResponseUtils.success(response);
    }

    @GetMapping
    public ResponseData<List<JobResponse>> getJobsByStatus(@RequestParam JobStatus status) {
        List<JobResponse> responses = jobStoreService.getJobsByStatus(status);
        return ResponseUtils.success(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseData<String> cancelJob(@PathVariable Long id) {
        jobStoreService.cancelJob(id);
        return ResponseUtils.success("Job cancelled successfully");
    }

    @PutMapping("/{id}/status")
    public ResponseData<String> updateJobStatus(@PathVariable Long id, @RequestParam JobStatus status) {
        jobStoreService.updateJobStatus(id, status);
        return ResponseUtils.success("Job status updated");
    }
}
