package com.distributed.jobscheduler.jobstore.controller;

import com.distributed.jobscheduler.common.dto.ScheduledJobResponse;
import com.distributed.jobscheduler.common.response.ResponseData;
import com.distributed.jobscheduler.common.response.ResponseUtils;
import com.distributed.jobscheduler.jobstore.service.JobStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/job-schedules")
@RequiredArgsConstructor
public class JobScheduleController {

    private final JobStoreService jobStoreService;

    @GetMapping("/due")
    public ResponseData<List<ScheduledJobResponse>> getDueSchedules(
            @RequestParam("asOf") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant asOf,
            @RequestParam("segments") List<Integer> segments) {
        if (segments == null) {
            segments = new ArrayList<>();
        }
        List<ScheduledJobResponse> schedules = jobStoreService.getScheduledJobs(asOf, segments);
        return ResponseUtils.success(schedules);
    }
}
