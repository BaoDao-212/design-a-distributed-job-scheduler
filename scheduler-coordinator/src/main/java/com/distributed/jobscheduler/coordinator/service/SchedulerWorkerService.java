package com.distributed.jobscheduler.coordinator.service;

import com.distributed.jobscheduler.coordinator.entity.SchedulerWorkerEntity;

import java.util.List;

public interface SchedulerWorkerService {

    SchedulerWorkerEntity registerWorker(String workerId);

    SchedulerWorkerEntity heartbeat(String workerId);

    List<SchedulerWorkerEntity> getActiveWorkers();
}
