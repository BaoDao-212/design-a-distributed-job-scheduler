package com.distributed.jobscheduler.worker.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchedulerWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerWorkerApplication.class, args);
    }
}
