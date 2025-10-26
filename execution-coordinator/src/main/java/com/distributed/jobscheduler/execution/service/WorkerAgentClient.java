package com.distributed.jobscheduler.execution.service;

import com.distributed.jobscheduler.common.dto.JobDispatchEvent;
import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.execution.entity.ExecutionWorkerEntity;
import com.distributed.jobscheduler.execution.repository.ExecutionWorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerAgentClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutionWorkerRepository workerRepository;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8084;

    public boolean dispatchJob(String workerId, JobDispatchEvent event) {
        try {
            String workerUrl = getWorkerUrl(workerId);
            String url = workerUrl + "/api/worker-agent/dispatch";

            restTemplate.postForObject(url, event, ResponseData.class);
            log.info("Successfully dispatched job {} to worker {} at {}", event.getJobId(), workerId, workerUrl);
            return true;
        } catch (Exception e) {
            log.error("Failed to dispatch job {} to worker {}: {}", event.getJobId(), workerId, e.getMessage());
            return false;
        }
    }

    private String getWorkerUrl(String workerId) {
        return workerRepository.findByWorkerId(workerId)
                .map(worker -> {
                    String host = worker.getWorkerHost() != null ? worker.getWorkerHost() : DEFAULT_HOST;
                    int port = worker.getWorkerPort() != null ? worker.getWorkerPort() : DEFAULT_PORT;
                    return String.format("http://%s:%d", host, port);
                })
                .orElseGet(() -> {
                    log.warn("Worker {} not found in registry, using default URL", workerId);
                    return String.format("http://%s:%d", DEFAULT_HOST, DEFAULT_PORT);
                });
    }
}
