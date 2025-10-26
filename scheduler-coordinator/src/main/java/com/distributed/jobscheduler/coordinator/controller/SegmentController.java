package com.distributed.jobscheduler.coordinator.controller;

import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import com.distributed.jobscheduler.coordinator.service.SegmentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/segments")
@RequiredArgsConstructor
public class SegmentController {

    private final SegmentAssignmentService segmentAssignmentService;

    @PostMapping("/assign")
    public ResponseData<List<Integer>> assignSegments(@RequestParam String workerId,
                                                      @RequestParam int desiredSegments) {
        List<Integer> segments = segmentAssignmentService.assignSegments(workerId, desiredSegments);
        return ResponseUtils.success(segments);
    }

    @DeleteMapping("/release")
    public ResponseData<String> releaseSegments(@RequestParam String workerId) {
        segmentAssignmentService.releaseSegments(workerId);
        return ResponseUtils.success("Segments released");
    }
}
