package com.distributed.jobscheduler.execution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExecutionCoordinatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutionCoordinatorApplication.class, args);
    }
}
