package com.distributed.jobscheduler.execution.repository;

import com.distributed.jobscheduler.execution.entity.ExecutionAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionAssignmentRepository extends JpaRepository<ExecutionAssignmentEntity, Long> {

    List<ExecutionAssignmentEntity> findByWorkerId(String workerId);

    List<ExecutionAssignmentEntity> findByJobId(Long jobId);
}
