package com.distributed.jobscheduler.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerAgentApplication.class, args);
    }
}
