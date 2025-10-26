package com.distributed.jobscheduler.execution.service;

import com.distributed.jobscheduler.common.dto.JobDispatchEvent;
import com.distributed.jobscheduler.common.enums.JobStatus;
import com.distributed.jobscheduler.common.enums.WorkerStatus;
import com.distributed.jobscheduler.execution.entity.ExecutionAssignmentEntity;
import com.distributed.jobscheduler.execution.entity.ExecutionWorkerEntity;
import com.distributed.jobscheduler.execution.entity.ReassignmentEventEntity;
import com.distributed.jobscheduler.execution.repository.ExecutionAssignmentRepository;
import com.distributed.jobscheduler.execution.repository.ExecutionWorkerRepository;
import com.distributed.jobscheduler.execution.repository.ReassignmentEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExecutionMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionMonitoringService.class);
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;
    private static final int MAX_REASSIGNMENT_ATTEMPTS = 3;

    private final ExecutionWorkerRepository executionWorkerRepository;
    private final ExecutionAssignmentRepository executionAssignmentRepository;
    private final ReassignmentEventRepository reassignmentEventRepository;
    private final WorkerAgentClient workerAgentClient;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void monitorWorkerHealth() {
        log.debug("Monitoring worker health");

        List<ExecutionWorkerEntity> workers = executionWorkerRepository.findByStatus(WorkerStatus.ONLINE);
        Instant now = Instant.now();

        for (ExecutionWorkerEntity worker : workers) {
            if (worker.getLastHeartbeat() != null &&
                    Duration.between(worker.getLastHeartbeat(), now).getSeconds() > HEARTBEAT_TIMEOUT_SECONDS) {

                log.warn("Worker {} is unhealthy. Last heartbeat: {}", worker.getWorkerId(), worker.getLastHeartbeat());
                worker.setStatus(WorkerStatus.UNHEALTHY);
                executionWorkerRepository.save(worker);

                reassignJobsFromWorker(worker.getWorkerId());
            }
        }
    }

    private void reassignJobsFromWorker(String failedWorkerId) {
        log.info("Starting job reassignment from failed worker: {}", failedWorkerId);

        List<ExecutionAssignmentEntity> runningAssignments =
                executionAssignmentRepository.findByWorkerIdAndStatus(failedWorkerId, JobStatus.RUNNING);

        if (runningAssignments.isEmpty()) {
            log.info("No running jobs to reassign from worker {}", failedWorkerId);
            return;
        }

        log.info("Found {} running jobs to reassign from worker {}", runningAssignments.size(), failedWorkerId);

        for (ExecutionAssignmentEntity assignment : runningAssignments) {
            try {
                reassignJob(assignment, failedWorkerId);
            } catch (Exception e) {
                log.error("Failed to reassign job {} from worker {}: {}",
                        assignment.getJobId(), failedWorkerId, e.getMessage(), e);
            }
        }
    }

    private void reassignJob(ExecutionAssignmentEntity assignment, String failedWorkerId) {
        Long jobId = assignment.getJobId();
        String checkpointData = assignment.getCheckpointData();

        log.info("Attempting to reassign job {} from worker {}", jobId, failedWorkerId);

        Optional<ExecutionWorkerEntity> targetWorkerOpt = selectBestWorkerForReassignment();

        if (targetWorkerOpt.isEmpty()) {
            log.error("No healthy worker available for reassigning job {}", jobId);
            recordReassignmentEvent(jobId, failedWorkerId, null,
                    checkpointData, false, "No healthy worker available");
            return;
        }

        ExecutionWorkerEntity targetWorker = targetWorkerOpt.get();
        String targetWorkerId = targetWorker.getWorkerId();

        log.info("Selected worker {} for reassigning job {}", targetWorkerId, jobId);

        JobDispatchEvent dispatchEvent = JobDispatchEvent.builder()
                .jobId(jobId)
                .jobName("Reassigned Job " + jobId)
                .payload(checkpointData != null ? checkpointData : "")
                .scheduledTime(Instant.now())
                .maxRetries(MAX_REASSIGNMENT_ATTEMPTS)
                .currentRetryCount(0)
                .build();

        boolean dispatchSuccess = workerAgentClient.dispatchJob(targetWorkerId, dispatchEvent);

        if (dispatchSuccess) {
            assignment.setWorkerId(targetWorkerId);
            assignment.setStatus(JobStatus.RUNNING);
            executionAssignmentRepository.save(assignment);

            targetWorker.setCurrentLoad(targetWorker.getCurrentLoad() + 1);
            executionWorkerRepository.save(targetWorker);

            log.info("Successfully reassigned job {} from {} to {}", jobId, failedWorkerId, targetWorkerId);
            recordReassignmentEvent(jobId, failedWorkerId, targetWorkerId,
                    checkpointData, true, null);
        } else {
            log.error("Failed to dispatch job {} to worker {}", jobId, targetWorkerId);
            recordReassignmentEvent(jobId, failedWorkerId, targetWorkerId,
                    checkpointData, false, "Dispatch failed");
        }
    }

    private Optional<ExecutionWorkerEntity> selectBestWorkerForReassignment() {
        List<ExecutionWorkerEntity> healthyWorkers = executionWorkerRepository.findByStatus(WorkerStatus.ONLINE);

        return healthyWorkers.stream()
                .filter(w -> w.getCapacity() != null && w.getCurrentLoad() != null)
                .filter(w -> w.getCurrentLoad() < w.getCapacity())
                .filter(w -> w.getLastHeartbeat() != null)
                .filter(w -> Duration.between(w.getLastHeartbeat(), Instant.now()).getSeconds() < 45)
                .min(Comparator.comparing(w -> w.getCurrentLoad().doubleValue() / w.getCapacity().doubleValue()));
    }

    private void recordReassignmentEvent(Long jobId, String fromWorkerId, String toWorkerId,
                                          String checkpointData, boolean success, String errorMessage) {
        ReassignmentEventEntity event = new ReassignmentEventEntity();
        event.setJobId(jobId);
        event.setFromWorkerId(fromWorkerId);
        event.setToWorkerId(toWorkerId);
        event.setReason("Worker failure detected");
        event.setCheckpointData(checkpointData);
        event.setSuccess(success);
        event.setErrorMessage(errorMessage);
        reassignmentEventRepository.save(event);

        log.info("Recorded reassignment event for job {} from {} to {} - Success: {}",
                jobId, fromWorkerId, toWorkerId, success);
    }
}
