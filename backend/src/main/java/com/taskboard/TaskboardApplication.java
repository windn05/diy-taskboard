package com.taskboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

// 오래된 system_logs 주기 삭제에 스케줄러 필요 (LogService.purgeOldLogs)
@EnableScheduling
@SpringBootApplication
public class TaskboardApplication {
    public static void main(String[] args) {
        // 저장 시각은 실행 환경과 무관하게 UTC 기준 (로컬 윈도우는 KST, 서버 컨테이너는 UTC라 섞이던 문제)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(TaskboardApplication.class, args);
    }
}
