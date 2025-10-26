package com.distributed.jobscheduler.jobstore.service;

import com.distributed.jobscheduler.common.dto.ScheduledJobResponse;
import com.distributed.jobscheduler.common.enums.JobStatus;
import com.distributed.jobscheduler.jobstore.dto.JobResponse;
import com.distributed.jobscheduler.jobstore.dto.JobSubmissionRequest;
import com.distributed.jobscheduler.jobstore.entity.JobEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobStoreService {

    JobResponse submitJob(JobSubmissionRequest request);

    Optional<JobResponse> getJobById(Long id);

    List<JobResponse> getJobsByStatus(JobStatus status);

    void cancelJob(Long jobId);

    void updateJobStatus(Long jobId, JobStatus status);

    List<ScheduledJobResponse> getScheduledJobs(Instant currentTime, List<Integer> segments);

    JobEntity getJobEntityById(Long jobId);
}
