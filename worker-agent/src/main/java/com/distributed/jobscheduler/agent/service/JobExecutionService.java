package com.distributed.jobscheduler.agent.service;

import com.distributed.jobscheduler.common.dto.JobDispatchEvent;
import com.distributed.jobscheduler.common.enums.JobStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionService.class);

    @Value("${worker.agent.concurrency-limit:10}")
    private int concurrencyLimit;

    @Value("${worker.agent.worker-id}")
    private String workerId;

    @Value("${worker.agent.job-store-url}")
    private String jobStoreUrl;

    private final Semaphore concurrencyLimiter = new Semaphore(10);
    private final RestTemplate restTemplate = new RestTemplate();

    public void executeJob(JobDispatchEvent event) {
        try {
            if (!concurrencyLimiter.tryAcquire()) {
                log.warn("Concurrency limit reached. Job {} cannot be executed now", event.getJobId());
                return;
            }

            log.info("Executing job {}: {}", event.getJobId(), event.getJobName());

            updateJobStatus(event.getJobId(), JobStatus.RUNNING);

            simulateJobExecution(event);

            updateJobStatus(event.getJobId(), JobStatus.COMPLETED);

            log.info("Job {} completed successfully", event.getJobId());

        } catch (Exception e) {
            log.error("Job {} failed: {}", event.getJobId(), e.getMessage());
            updateJobStatus(event.getJobId(), JobStatus.FAILED);
        } finally {
            concurrencyLimiter.release();
        }
    }

    private void simulateJobExecution(JobDispatchEvent event) throws InterruptedException {
        log.debug("Processing job {} payload: {}", event.getJobId(), event.getPayload());
        Thread.sleep(1000);
    }

    private void updateJobStatus(Long jobId, JobStatus status) {
        try {
            String url = jobStoreUrl + "/api/jobs/" + jobId + "/status?status=" + status;
            restTemplate.put(url, null);
            log.debug("Updated job {} status to {}", jobId, status);
        } catch (Exception e) {
            log.error("Failed to update job status", e);
        }
    }
}
