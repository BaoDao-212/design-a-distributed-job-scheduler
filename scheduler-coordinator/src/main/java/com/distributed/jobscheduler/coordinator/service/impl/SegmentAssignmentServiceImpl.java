package com.distributed.jobscheduler.coordinator.service.impl;

import com.distributed.jobscheduler.coordinator.entity.SegmentAssignmentEntity;
import com.distributed.jobscheduler.coordinator.repository.SegmentAssignmentRepository;
import com.distributed.jobscheduler.coordinator.service.SegmentAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SegmentAssignmentServiceImpl implements SegmentAssignmentService {

    private static final int TOTAL_SEGMENTS = 100;
    private final SegmentAssignmentRepository segmentAssignmentRepository;

    @Override
    @Transactional
    public List<Integer> assignSegments(String workerId, int desiredSegments) {
        List<SegmentAssignmentEntity> allAssignments = segmentAssignmentRepository.findAll();
        Set<Integer> assignedSegments = allAssignments.stream()
                .map(SegmentAssignmentEntity::getSegment)
                .collect(Collectors.toSet());

        List<Integer> newAssignments = new ArrayList<>();
        for (int i = 0; i < TOTAL_SEGMENTS && newAssignments.size() < desiredSegments; i++) {
            if (!assignedSegments.contains(i)) {
                SegmentAssignmentEntity assignment = new SegmentAssignmentEntity();
                assignment.setWorkerId(workerId);
                assignment.setSegment(i);
                segmentAssignmentRepository.save(assignment);
                newAssignments.add(i);
            }
        }

        return newAssignments;
    }

    @Override
    @Transactional
    public void releaseSegments(String workerId) {
        List<SegmentAssignmentEntity> assignments = segmentAssignmentRepository.findByWorkerId(workerId);
        segmentAssignmentRepository.deleteAll(assignments);
    }
}
