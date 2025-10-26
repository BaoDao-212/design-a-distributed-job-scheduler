package com.distributed.jobscheduler.jobstore.service.impl;

import com.distributed.jobscheduler.common.dto.ScheduledJobResponse;
import com.distributed.jobscheduler.common.enums.JobStatus;
import com.distributed.jobscheduler.jobstore.dto.JobResponse;
import com.distributed.jobscheduler.jobstore.dto.JobSubmissionRequest;
import com.distributed.jobscheduler.jobstore.entity.JobEntity;
import com.distributed.jobscheduler.jobstore.entity.JobScheduleEntity;
import com.distributed.jobscheduler.jobstore.repository.JobRepository;
import com.distributed.jobscheduler.jobstore.repository.JobScheduleRepository;
import com.distributed.jobscheduler.jobstore.service.JobStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobStoreServiceImpl implements JobStoreService {

    private final JobRepository jobRepository;
    private final JobScheduleRepository jobScheduleRepository;

    @Override
    @Transactional
    public JobResponse submitJob(JobSubmissionRequest request) {
        JobEntity jobEntity = new JobEntity();
        jobEntity.setJobName(request.getJobName());
        jobEntity.setUserId(request.getUserId());
        jobEntity.setFrequency(request.getFrequency());
        jobEntity.setExecutionTime(request.getExecutionTime());
        jobEntity.setPayload(request.getPayload());
        jobEntity.setMaxRetries(request.getMaxRetries());
        jobEntity.setStatus(JobStatus.PENDING);
        jobEntity = jobRepository.save(jobEntity);

        JobScheduleEntity schedule = new JobScheduleEntity();
        schedule.setJobId(jobEntity.getId());
        schedule.setSegment(request.getSegment());
        schedule.setNextRunTime(request.getExecutionTime());
        jobScheduleRepository.save(schedule);

        JobResponse response = new JobResponse();
        response.setId(jobEntity.getId());
        response.setJobName(jobEntity.getJobName());
        response.setUserId(jobEntity.getUserId());
        response.setFrequency(jobEntity.getFrequency());
        response.setExecutionTime(jobEntity.getExecutionTime());
        response.setPayload(jobEntity.getPayload());
        response.setRetryCount(jobEntity.getRetryCount());
        response.setMaxRetries(jobEntity.getMaxRetries());
        response.setStatus(jobEntity.getStatus());
        response.setCreatedAt(jobEntity.getCreatedAt());
        response.setUpdatedAt(jobEntity.getUpdatedAt());
        return response;
    }

    @Override
    public Optional<JobResponse> getJobById(Long id) {
        return jobRepository.findById(id).map(jobEntity -> {
            JobResponse response = new JobResponse();
            response.setId(jobEntity.getId());
            response.setJobName(jobEntity.getJobName());
            response.setUserId(jobEntity.getUserId());
            response.setFrequency(jobEntity.getFrequency());
            response.setExecutionTime(jobEntity.getExecutionTime());
            response.setPayload(jobEntity.getPayload());
            response.setRetryCount(jobEntity.getRetryCount());
            response.setMaxRetries(jobEntity.getMaxRetries());
            response.setStatus(jobEntity.getStatus());
            response.setCreatedAt(jobEntity.getCreatedAt());
            response.setUpdatedAt(jobEntity.getUpdatedAt());
            return response;
        });
    }

    @Override
    public List<JobResponse> getJobsByStatus(JobStatus status) {
        return jobRepository.findByStatus(status).stream().map(jobEntity -> {
            JobResponse response = new JobResponse();
            response.setId(jobEntity.getId());
            response.setJobName(jobEntity.getJobName());
            response.setUserId(jobEntity.getUserId());
            response.setFrequency(jobEntity.getFrequency());
            response.setExecutionTime(jobEntity.getExecutionTime());
            response.setPayload(jobEntity.getPayload());
            response.setRetryCount(jobEntity.getRetryCount());
            response.setMaxRetries(jobEntity.getMaxRetries());
            response.setStatus(jobEntity.getStatus());
            response.setCreatedAt(jobEntity.getCreatedAt());
            response.setUpdatedAt(jobEntity.getUpdatedAt());
            return response;
        }).toList();
    }

    @Override
    @Transactional
    public void cancelJob(Long jobId) {
        jobRepository.findById(jobId).ifPresent(jobEntity -> {
            jobEntity.setStatus(JobStatus.CANCELLED);
            jobRepository.save(jobEntity);
        });
    }

    @Override
    @Transactional
    public void updateJobStatus(Long jobId, JobStatus status) {
        jobRepository.findById(jobId).ifPresent(jobEntity -> {
            jobEntity.setStatus(status);
            jobRepository.save(jobEntity);
        });
    }

    @Override
    public List<ScheduledJobResponse> getScheduledJobs(Instant currentTime, List<Integer> segments) {
        return jobScheduleRepository.findScheduledJobs(currentTime, segments)
                .stream()
                .map(schedule -> {
                    JobEntity job = jobRepository.findById(schedule.getJobId())
                            .orElseThrow(() -> new IllegalArgumentException("Job not found"));
                    return ScheduledJobResponse.builder()
                            .jobId(job.getId())
                            .scheduleId(schedule.getId())
                            .jobName(job.getJobName())
                            .frequency(job.getFrequency())
                            .status(job.getStatus())
                            .nextRunTime(schedule.getNextRunTime())
                            .segment(schedule.getSegment())
                            .payload(job.getPayload())
                            .build();
                })
                .toList();
    }

    @Override
    public JobEntity getJobEntityById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    }
}
