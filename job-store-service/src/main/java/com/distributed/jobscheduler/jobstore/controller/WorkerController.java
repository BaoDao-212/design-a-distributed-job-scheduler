package com.distributed.jobscheduler.jobstore.controller;

import com.distributed.jobscheduler.common.enums.WorkerStatus;
import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import com.distributed.jobscheduler.jobstore.dto.WorkerRegistrationRequest;
import com.distributed.jobscheduler.jobstore.entity.WorkerEntity;
import com.distributed.jobscheduler.jobstore.repository.WorkerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerRepository workerRepository;

    @PostMapping
    public ResponseData<WorkerEntity> registerWorker(@Valid @RequestBody WorkerRegistrationRequest request) {
        WorkerEntity worker = workerRepository.findByWorkerId(request.getWorkerId())
                .orElseGet(WorkerEntity::new);
        worker.setWorkerId(request.getWorkerId());
        worker.setSegment(request.getSegment());
        worker.setCapacity(request.getCapacity());
        worker.setStatus(WorkerStatus.ONLINE);
        worker.setCurrentLoad(0);
        workerRepository.save(worker);
        return ResponseUtils.success(worker);
    }

    @GetMapping
    public ResponseData<List<WorkerEntity>> getAllWorkers() {
        List<WorkerEntity> workers = workerRepository.findAll();
        return ResponseUtils.success(workers);
    }

    @PutMapping("/{workerId}/status")
    public ResponseData<String> updateWorkerStatus(@PathVariable String workerId,
                                                   @RequestParam WorkerStatus status) {
        workerRepository.findByWorkerId(workerId).ifPresent(worker -> {
            worker.setStatus(status);
            workerRepository.save(worker);
        });
        return ResponseUtils.success("Worker status updated");
    }
}
