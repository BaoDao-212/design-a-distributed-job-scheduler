package com.distributed.jobscheduler.jobstore.repository;

import com.distributed.jobscheduler.jobstore.entity.JobScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobScheduleRepository extends JpaRepository<JobScheduleEntity, Long> {

    @Query("SELECT js FROM JobScheduleEntity js WHERE js.nextRunTime <= :currentTime AND js.segment IN :segments")
    List<JobScheduleEntity> findScheduledJobs(@Param("currentTime") Instant currentTime,
                                               @Param("segments") List<Integer> segments);

    Optional<JobScheduleEntity> findByJobId(Long jobId);
}
