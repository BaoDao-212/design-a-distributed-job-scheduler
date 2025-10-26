package com.distributed.jobscheduler.jobstore.repository;

import com.distributed.jobscheduler.jobstore.entity.WorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerRepository extends JpaRepository<WorkerEntity, Long> {

    Optional<WorkerEntity> findByWorkerId(String workerId);
}
