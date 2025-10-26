package com.distributed.jobscheduler.coordinator.repository;

import com.distributed.jobscheduler.coordinator.entity.CoordinatorNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoordinatorNodeRepository extends JpaRepository<CoordinatorNodeEntity, Long> {

    Optional<CoordinatorNodeEntity> findByNodeId(String nodeId);

    Optional<CoordinatorNodeEntity> findByLeader(boolean leader);
}
