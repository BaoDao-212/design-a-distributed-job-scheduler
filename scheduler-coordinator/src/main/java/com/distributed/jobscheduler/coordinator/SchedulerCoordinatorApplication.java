package com.distributed.jobscheduler.coordinator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SchedulerCoordinatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerCoordinatorApplication.class, args);
    }
}
