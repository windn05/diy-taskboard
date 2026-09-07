package com.taskboard.service;

import com.taskboard.domain.SystemLog;
import com.taskboard.dto.LogDtos.LogEntry;
import com.taskboard.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

@Service
public class LogService {

    public static final String LOG_TOPIC = "/topic/admin/logs";

    private static final List<String> LEVELS = List.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    private static final Set<String> PERSISTED_LEVELS = Set.of("WARN", "ERROR");

    /**
     * 로그를 저장·전송하는 과정에서 다시 로그가 발생하면 무한 재귀가 된다.
     * (예: DB 저장 실패 → 에러 로그 → 다시 저장 시도)
     */
    private static final ThreadLocal<Boolean> HANDLING = new ThreadLocal<>();

    private final SystemLogRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final int bufferSize;
    private final Deque<LogEntry> buffer = new ArrayDeque<>();

    public LogService(SystemLogRepository repository,
                      SimpMessagingTemplate messagingTemplate,
                      @Value("${taskboard.log.buffer-size:500}") int bufferSize) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.bufferSize = bufferSize;
    }

    public void record(LogEntry entry) {
        if (Boolean.TRUE.equals(HANDLING.get())) return;
        HANDLING.set(Boolean.TRUE);
        try {
            addToBuffer(entry);
            if (PERSISTED_LEVELS.contains(entry.level())) {
                repository.save(toEntity(entry));
            }
            messagingTemplate.convertAndSend(LOG_TOPIC, entry);
        } catch (Exception e) {
            // 로깅 실패가 애플리케이션을 멈추게 해선 안 된다. 여기서 다시 로그를 남기면 재귀가 된다.
        } finally {
            HANDLING.remove();
        }
    }

    /** 메모리 버퍼의 최근 로그 (최신순). */
    public List<LogEntry> recent(String minLevel, int limit) {
        List<LogEntry> snapshot;
        synchronized (buffer) {
            snapshot = new ArrayList<>(buffer);
        }
        Collections.reverse(snapshot);
        return snapshot.stream()
                .filter(entry -> meetsLevel(entry.level(), minLevel))
                .limit(limit)
                .toList();
    }

    /** DB에 영속된 로그 (WARN/ERROR만 저장되므로 그 범위 안에서 조회된다). */
    public List<LogEntry> history(String minLevel, int limit) {
        List<String> levels = PERSISTED_LEVELS.stream().filter(level -> meetsLevel(level, minLevel)).toList();
        if (levels.isEmpty()) return List.of();
        return repository.findByLevelInOrderByIdDesc(levels, PageRequest.of(0, limit)).stream()
                .map(this::toEntry)
                .toList();
    }

    private void addToBuffer(LogEntry entry) {
        synchronized (buffer) {
            buffer.addLast(entry);
            while (buffer.size() > bufferSize) {
                buffer.removeFirst();
            }
        }
    }

    private boolean meetsLevel(String level, String minLevel) {
        return LEVELS.indexOf(level) >= LEVELS.indexOf(minLevel);
    }

    private SystemLog toEntity(LogEntry entry) {
        return SystemLog.builder()
                .level(entry.level())
                .loggerName(entry.loggerName())
                .threadName(entry.threadName())
                .message(entry.message())
                .stackTrace(entry.stackTrace())
                .loggedAt(entry.loggedAt())
                .build();
    }

    private LogEntry toEntry(SystemLog log) {
        return new LogEntry(log.getLevel(), log.getLoggerName(), log.getThreadName(),
                log.getMessage(), log.getStackTrace(), log.getLoggedAt());
    }
}
