package com.distributed.jobscheduler.coordinator.repository;

import com.distributed.jobscheduler.coordinator.entity.SchedulerWorkerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulerWorkerRepository extends JpaRepository<SchedulerWorkerEntity, Long> {

    Optional<SchedulerWorkerEntity> findByWorkerId(String workerId);
}
