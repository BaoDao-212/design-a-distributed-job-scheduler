package com.distributed.jobscheduler.coordinator.service;

import com.distributed.jobscheduler.coordinator.entity.CoordinatorNodeEntity;

public interface CoordinatorService {

    CoordinatorNodeEntity registerCoordinator(String nodeId, Integer priority);

    CoordinatorNodeEntity getLeader();

    void heartbeat(String nodeId);
}
