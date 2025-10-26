package com.distributed.jobscheduler.worker.scheduler.controller;

import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import com.distributed.jobscheduler.worker.scheduler.config.WorkerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scheduler-worker")
@RequiredArgsConstructor
public class WorkerAssignmentController {

    private final WorkerConfig workerConfig;

    @PostMapping("/segments")
    public ResponseData<String> updateSegments(@RequestBody List<Integer> segments) {
        workerConfig.setAssignedSegments(segments);
        return ResponseUtils.success("Segments updated");
    }

    @GetMapping("/segments")
    public ResponseData<List<Integer>> getSegments() {
        return ResponseUtils.success(workerConfig.getAssignedSegments());
    }
}
