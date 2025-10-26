package com.distributed.jobscheduler.execution.controller;

import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import com.distributed.jobscheduler.execution.entity.ExecutionWorkerEntity;
import com.distributed.jobscheduler.execution.repository.ExecutionWorkerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/execution-coordinator")
@RequiredArgsConstructor
public class ExecutionCoordinatorController {

    private final ExecutionWorkerRepository workerRepository;

    @PostMapping("/workers/{workerId}")
    public ResponseData<ExecutionWorkerEntity> registerWorker(@PathVariable String workerId,
                                                               @RequestParam(required = false) Integer capacity,
                                                               @RequestParam(required = false) String host,
                                                               @RequestParam(required = false) Integer port) {
        ExecutionWorkerEntity worker = workerRepository.findByWorkerId(workerId)
                .orElseGet(() -> {
                    ExecutionWorkerEntity entity = new ExecutionWorkerEntity();
                    entity.setWorkerId(workerId);
                    return entity;
                });
        worker.setCapacity(capacity);
        worker.setCurrentLoad(0);
        worker.setWorkerHost(host);
        worker.setWorkerPort(port);
        worker.setLastHeartbeat(Instant.now());
        workerRepository.save(worker);
        return ResponseUtils.success(worker);
    }

    @PostMapping("/workers/{workerId}/heartbeat")
    public ResponseData<String> heartbeat(@PathVariable String workerId,
                                          @RequestParam(required = false) Integer currentLoad,
                                          @RequestParam(required = false) Integer capacity) {
        ExecutionWorkerEntity worker = workerRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        worker.setLastHeartbeat(Instant.now());
        if (currentLoad != null) {
            worker.setCurrentLoad(currentLoad);
        }
        if (capacity != null) {
            worker.setCapacity(capacity);
        }
        workerRepository.save(worker);
        return ResponseUtils.success("Heartbeat recorded");
    }

    @GetMapping("/workers")
    public ResponseData<List<ExecutionWorkerEntity>> listWorkers() {
        return ResponseUtils.success(workerRepository.findAll());
    }
}
