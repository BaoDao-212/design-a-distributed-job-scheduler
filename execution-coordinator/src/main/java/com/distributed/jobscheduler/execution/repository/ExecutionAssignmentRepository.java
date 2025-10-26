package com.distributed.jobscheduler.execution.repository;

import com.distributed.jobscheduler.common.enums.JobStatus;
import com.distributed.jobscheduler.execution.entity.ExecutionAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExecutionAssignmentRepository extends JpaRepository<ExecutionAssignmentEntity, Long> {

    List<ExecutionAssignmentEntity> findByWorkerId(String workerId);

    List<ExecutionAssignmentEntity> findByWorkerIdAndStatus(String workerId, JobStatus status);

    Optional<ExecutionAssignmentEntity> findByJobId(Long jobId);
}
