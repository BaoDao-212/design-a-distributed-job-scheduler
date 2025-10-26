package com.distributed.jobscheduler.jobstore.repository;

import com.distributed.jobscheduler.common.enums.JobStatus;
import com.distributed.jobscheduler.jobstore.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<JobEntity, Long> {

    List<JobEntity> findByStatus(JobStatus status);

    List<JobEntity> findByExecutionTimeBeforeAndStatus(Instant time, JobStatus status);
}
