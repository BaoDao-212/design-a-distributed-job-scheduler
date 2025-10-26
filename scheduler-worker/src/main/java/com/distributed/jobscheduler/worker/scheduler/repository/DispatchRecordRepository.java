package com.distributed.jobscheduler.worker.scheduler.repository;

import com.distributed.jobscheduler.worker.scheduler.entity.DispatchRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchRecordRepository extends JpaRepository<DispatchRecordEntity, Long> {
}
