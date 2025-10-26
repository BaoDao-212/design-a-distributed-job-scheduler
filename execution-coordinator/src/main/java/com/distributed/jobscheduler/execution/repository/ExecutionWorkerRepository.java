package com.distributed.jobscheduler.execution.repository;

import com.distributed.jobscheduler.common.enums.WorkerStatus;
import com.distributed.jobscheduler.execution.entity.ExecutionWorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExecutionWorkerRepository extends JpaRepository<ExecutionWorkerEntity, Long> {

    Optional<ExecutionWorkerEntity> findByWorkerId(String workerId);

    List<ExecutionWorkerEntity> findByStatus(WorkerStatus status);
}
