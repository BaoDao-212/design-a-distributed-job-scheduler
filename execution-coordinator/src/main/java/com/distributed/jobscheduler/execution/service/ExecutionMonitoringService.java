package com.distributed.jobscheduler.execution.service;

import com.distributed.jobscheduler.common.enums.WorkerStatus;
import com.distributed.jobscheduler.execution.entity.ExecutionAssignmentEntity;
import com.distributed.jobscheduler.execution.entity.ExecutionWorkerEntity;
import com.distributed.jobscheduler.execution.repository.ExecutionAssignmentRepository;
import com.distributed.jobscheduler.execution.repository.ExecutionWorkerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExecutionMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionMonitoringService.class);
    private static final int HEARTBEAT_TIMEOUT_SECONDS = 60;

    private final ExecutionWorkerRepository executionWorkerRepository;
    private final ExecutionAssignmentRepository executionAssignmentRepository;

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

    private void reassignJobsFromWorker(String workerId) {
        List<ExecutionAssignmentEntity> assignments = executionAssignmentRepository.findByWorkerId(workerId);

        for (ExecutionAssignmentEntity assignment : assignments) {
            log.info("Reassigning job {} from failed worker {}", assignment.getJobId(), workerId);
        }
    }
}
