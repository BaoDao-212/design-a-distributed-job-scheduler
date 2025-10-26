package com.distributed.jobscheduler.worker.scheduler.service;

import com.distributed.jobscheduler.common.dto.JobDispatchEvent;
import com.distributed.jobscheduler.common.dto.ScheduledJobResponse;
import com.distributed.jobscheduler.common.enums.JobStatus;
import com.distributed.jobscheduler.worker.scheduler.config.WorkerConfig;
import com.distributed.jobscheduler.worker.scheduler.entity.DispatchRecordEntity;
import com.distributed.jobscheduler.worker.scheduler.repository.DispatchRecordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(JobSchedulerService.class);

    private final WorkerConfig workerConfig;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void scanAndDispatchJobs() {
        if (workerConfig.getAssignedSegments() == null || workerConfig.getAssignedSegments().isEmpty()) {
            log.debug("No segments assigned to this worker");
            return;
        }

        log.info("Scanning jobs for segments: {}", workerConfig.getAssignedSegments());
        Instant now = Instant.now();

        try {
            String baseUrl = workerConfig.getJobStoreUrl() + "/api/job-schedules/due";
            var builder = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("asOf", now);
            workerConfig.getAssignedSegments().forEach(segment -> builder.queryParam("segments", segment));

            ScheduledJobResponse[] scheduledJobs = restTemplate.getForObject(builder.toUriString(), ScheduledJobResponse[].class);

            if (scheduledJobs != null) {
                for (ScheduledJobResponse scheduledJob : scheduledJobs) {
                    dispatchJob(scheduledJob);
                }
            }

            log.info("Job scan cycle completed");
        } catch (Exception e) {
            log.error("Error during job scanning and dispatching", e);
        }
    }

    private void dispatchJob(ScheduledJobResponse scheduledJob) {
        JobDispatchEvent event = JobDispatchEvent.builder()
                .jobId(scheduledJob.getJobId())
                .jobName(scheduledJob.getJobName())
                .payload(scheduledJob.getPayload())
                .scheduledTime(scheduledJob.getNextRunTime())
                .build();

        log.info("Dispatching job {}: {}", event.getJobId(), event.getJobName());

        DispatchRecordEntity record = new DispatchRecordEntity();
        record.setJobId(scheduledJob.getJobId());
        record.setSegment(scheduledJob.getSegment());
        record.setPayload(scheduledJob.getPayload());
        dispatchRecordRepository.save(record);

        updateJobStatus(scheduledJob.getJobId(), JobStatus.SCHEDULED);
    }

    private void updateJobStatus(Long jobId, JobStatus status) {
        try {
            String url = workerConfig.getJobStoreUrl() + "/api/jobs/" + jobId + "/status?status=" + status;
            restTemplate.put(url, null);
            log.info("Updated job {} status to {}", jobId, status);
        } catch (Exception e) {
            log.error("Failed to update job status", e);
        }
    }
}
