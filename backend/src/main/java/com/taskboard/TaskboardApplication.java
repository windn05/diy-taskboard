package com.taskboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 오래된 system_logs를 주기적으로 지우려면 스케줄러가 필요하다 (LogService.purgeOldLogs).
@EnableScheduling
@SpringBootApplication
public class TaskboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskboardApplication.class, args);
    }
}
