package com.taskboard.service;

import com.taskboard.domain.SystemLog;
import com.taskboard.dto.LogDtos.LogEntry;
import com.taskboard.repository.SystemLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final SystemLogRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final int bufferSize;
    private final int retentionDays;
    private final Deque<LogEntry> buffer = new ArrayDeque<>();

    public LogService(SystemLogRepository repository,
                      SimpMessagingTemplate messagingTemplate,
                      @Value("${taskboard.log.buffer-size:500}") int bufferSize,
                      @Value("${taskboard.log.retention-days:7}") int retentionDays) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.bufferSize = bufferSize;
        this.retentionDays = retentionDays;
    }

    /**
     * 보존 기간이 지난 영속 로그를 지운다. 메모리 버퍼는 {@link #addToBuffer}에서 이미 상한이 걸려 있지만
     * DB 쪽은 상한이 없어, 두지 않으면 무한히 늘어난다. 조회는 어차피 최신 N건만 하므로 오래된 행은
     * 보이지도 않으면서 디스크만 차지한다.
     *
     * <p>장애가 나면 요청마다 ERROR가 쌓여 유입이 급증하는데(스택트레이스 포함), 그때 디스크가 차면
     * 원래 장애가 복구된 뒤에도 DB가 못 쓰게 되는 2차 장애가 된다. 트래픽이 적은 새벽에 하루 한 번 돈다.
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeOldLogs() {
        int deleted = repository.deleteLoggedBefore(LocalDateTime.now().minusDays(retentionDays));
        // INFO는 DB에 영속되지 않으므로(PERSISTED_LEVELS) 이 로그가 다시 행을 만들지는 않는다.
        if (deleted > 0) log.info("보존 기간({}일)이 지난 시스템 로그 {}건을 삭제했다.", retentionDays, deleted);
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
