package com.taskboard.monitoring;

import java.time.LocalDateTime;

/** 시스템 로그 응답 */
public class LogDtos {

    /** 로그 한 건 */
    public record LogEntry(
            String level,
            String loggerName,
            String threadName,
            String message,
            String stackTrace,
            LocalDateTime loggedAt) {
    }
}
