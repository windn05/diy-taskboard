package com.taskboard.dto;

import java.time.LocalDateTime;

public class LogDtos {

    /** 링버퍼 저장 형식이자 조회 응답이자 STOMP 스트리밍 페이로드. */
    public record LogEntry(
            String level,
            String loggerName,
            String threadName,
            String message,
            String stackTrace,
            LocalDateTime loggedAt) {
    }
}
