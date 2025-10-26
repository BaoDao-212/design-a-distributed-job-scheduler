package com.distributed.jobscheduler.coordinator.service;

import java.util.List;

public interface SegmentAssignmentService {

    List<Integer> assignSegments(String workerId, int desiredSegments);

    void releaseSegments(String workerId);
}
