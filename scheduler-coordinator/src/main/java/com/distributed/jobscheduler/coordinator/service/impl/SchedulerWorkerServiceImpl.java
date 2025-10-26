package com.distributed.jobscheduler.coordinator.service.impl;

import com.distributed.jobscheduler.common.enums.WorkerStatus;
import com.distributed.jobscheduler.coordinator.entity.SchedulerWorkerEntity;
import com.distributed.jobscheduler.coordinator.repository.SchedulerWorkerRepository;
import com.distributed.jobscheduler.coordinator.service.SchedulerWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SchedulerWorkerServiceImpl implements SchedulerWorkerService {

    private final SchedulerWorkerRepository schedulerWorkerRepository;

    @Override
    @Transactional
    public SchedulerWorkerEntity registerWorker(String workerId) {
        SchedulerWorkerEntity worker = schedulerWorkerRepository.findByWorkerId(workerId)
                .orElseGet(() -> {
                    SchedulerWorkerEntity newWorker = new SchedulerWorkerEntity();
                    newWorker.setWorkerId(workerId);
                    newWorker.setStatus(WorkerStatus.ONLINE);
                    return newWorker;
                });

        worker.setLastHeartbeat(Instant.now());
        return schedulerWorkerRepository.save(worker);
    }

    @Override
    @Transactional
    public SchedulerWorkerEntity heartbeat(String workerId) {
        SchedulerWorkerEntity worker = schedulerWorkerRepository.findByWorkerId(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        worker.setLastHeartbeat(Instant.now());
        worker.setStatus(WorkerStatus.ONLINE);
        return schedulerWorkerRepository.save(worker);
    }

    @Override
    public List<SchedulerWorkerEntity> getActiveWorkers() {
        return schedulerWorkerRepository.findAll().stream()
                .filter(w -> w.getStatus() == WorkerStatus.ONLINE)
                .toList();
    }
}
