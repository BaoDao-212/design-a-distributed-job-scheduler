package com.distributed.jobscheduler.coordinator.repository;

import com.distributed.jobscheduler.coordinator.entity.SegmentAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SegmentAssignmentRepository extends JpaRepository<SegmentAssignmentEntity, Long> {

    List<SegmentAssignmentEntity> findByWorkerId(String workerId);
}
