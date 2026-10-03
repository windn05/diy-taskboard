package com.taskboard.monitoring;

import java.time.LocalDateTime;

/** 시스템 로그 */
public class LogDtos {

    /** 링버퍼 저장 형식이자 조회 응답이자 STOMP 스트리밍 페이로드 */
    public record LogEntry(
            String level,
            String loggerName,
            String threadName,
            String message,
            String stackTrace,
            LocalDateTime loggedAt) {
    }
}
