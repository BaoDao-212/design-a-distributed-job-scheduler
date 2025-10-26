package com.distributed.jobscheduler.jobstore.repository;

import com.distributed.jobscheduler.jobstore.entity.JobExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, Long> {

    List<JobExecutionEntity> findByJobId(Long jobId);
}
