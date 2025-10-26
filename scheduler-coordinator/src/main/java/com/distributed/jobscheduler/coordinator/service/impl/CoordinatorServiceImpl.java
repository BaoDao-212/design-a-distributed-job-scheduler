package com.distributed.jobscheduler.coordinator.service.impl;

import com.distributed.jobscheduler.coordinator.entity.CoordinatorNodeEntity;
import com.distributed.jobscheduler.coordinator.repository.CoordinatorNodeRepository;
import com.distributed.jobscheduler.coordinator.service.CoordinatorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CoordinatorServiceImpl implements CoordinatorService {

    private static final Logger log = LoggerFactory.getLogger(CoordinatorServiceImpl.class);

    private final CoordinatorNodeRepository coordinatorNodeRepository;

    @Override
    @Transactional
    public CoordinatorNodeEntity registerCoordinator(String nodeId, Integer priority) {
        CoordinatorNodeEntity node = coordinatorNodeRepository.findByNodeId(nodeId)
                .orElseGet(() -> {
                    CoordinatorNodeEntity newNode = new CoordinatorNodeEntity();
                    newNode.setNodeId(nodeId);
                    newNode.setPriority(priority);
                    newNode.setLeader(false);
                    return newNode;
                });

        node.setLastHeartbeat(Instant.now());

        node = coordinatorNodeRepository.save(node);

        if (!coordinatorNodeRepository.findByLeader(true).isPresent()) {
            node.setLeader(true);
            node = coordinatorNodeRepository.save(node);
            log.info("Node {} elected as leader", nodeId);
        }

        return node;
    }

    @Override
    public CoordinatorNodeEntity getLeader() {
        return coordinatorNodeRepository.findByLeader(true)
                .orElseThrow(() -> new IllegalStateException("No leader elected"));
    }

    @Override
    @Transactional
    public void heartbeat(String nodeId) {
        coordinatorNodeRepository.findByNodeId(nodeId).ifPresent(node -> {
            node.setLastHeartbeat(Instant.now());
            coordinatorNodeRepository.save(node);
        });
    }
}
