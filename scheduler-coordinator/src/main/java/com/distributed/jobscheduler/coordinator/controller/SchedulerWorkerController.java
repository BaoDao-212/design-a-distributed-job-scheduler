package com.distributed.jobscheduler.coordinator.controller;

import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import com.distributed.jobscheduler.coordinator.entity.SchedulerWorkerEntity;
import com.distributed.jobscheduler.coordinator.service.SchedulerWorkerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheduler-workers")
@RequiredArgsConstructor
public class SchedulerWorkerController {

    private final SchedulerWorkerService schedulerWorkerService;

    @PostMapping("/register")
    public ResponseData<SchedulerWorkerEntity> registerWorker(@RequestParam String workerId) {
        SchedulerWorkerEntity worker = schedulerWorkerService.registerWorker(workerId);
        return ResponseUtils.success(worker);
    }

    @PostMapping("/heartbeat")
    public ResponseData<SchedulerWorkerEntity> heartbeat(@RequestParam String workerId) {
        SchedulerWorkerEntity worker = schedulerWorkerService.heartbeat(workerId);
        return ResponseUtils.success(worker);
    }

    @GetMapping
    public ResponseData<List<SchedulerWorkerEntity>> getActiveWorkers() {
        List<SchedulerWorkerEntity> workers = schedulerWorkerService.getActiveWorkers();
        return ResponseUtils.success(workers);
    }
}
