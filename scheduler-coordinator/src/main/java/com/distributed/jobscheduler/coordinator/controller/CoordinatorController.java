package com.distributed.jobscheduler.coordinator.controller;

import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import com.distributed.jobscheduler.coordinator.entity.CoordinatorNodeEntity;
import com.distributed.jobscheduler.coordinator.service.CoordinatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coordinator")
@RequiredArgsConstructor
public class CoordinatorController {

    private final CoordinatorService coordinatorService;

    @PostMapping("/register")
    public ResponseData<CoordinatorNodeEntity> register(@RequestParam String nodeId,
                                                        @RequestParam Integer priority) {
        CoordinatorNodeEntity node = coordinatorService.registerCoordinator(nodeId, priority);
        return ResponseUtils.success(node);
    }

    @GetMapping("/leader")
    public ResponseData<CoordinatorNodeEntity> getLeader() {
        CoordinatorNodeEntity leader = coordinatorService.getLeader();
        return ResponseUtils.success(leader);
    }

    @PostMapping("/heartbeat")
    public ResponseData<String> heartbeat(@RequestParam String nodeId) {
        coordinatorService.heartbeat(nodeId);
        return ResponseUtils.success("Heartbeat recorded");
    }
}
