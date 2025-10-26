package com.distributed.jobscheduler.execution.repository;

import com.distributed.jobscheduler.execution.entity.ReassignmentEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReassignmentEventRepository extends JpaRepository<ReassignmentEventEntity, Long> {
}
