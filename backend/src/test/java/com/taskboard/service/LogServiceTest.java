package com.taskboard.service;

import com.taskboard.domain.SystemLog;
import com.taskboard.dto.LogDtos.LogEntry;
import com.taskboard.repository.SystemLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogServiceTest {

    private SystemLogRepository repository;
    private SimpMessagingTemplate messagingTemplate;
    private LogService logService;

    @BeforeEach
    void setUp() {
        repository = mock(SystemLogRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        logService = new LogService(repository, messagingTemplate, 3);
    }

    private LogEntry entry(String level, String message) {
        return new LogEntry(level, "com.taskboard.Test", "main", message, null, LocalDateTime.now());
    }

    @Test
    void 기록한_로그는_최신순으로_조회된다() {
        logService.record(entry("INFO", "첫번째"));
        logService.record(entry("INFO", "두번째"));

        assertThat(logService.recent("TRACE", 10))
                .extracting(LogEntry::message)
                .containsExactly("두번째", "첫번째");
    }

    @Test
    void 링버퍼는_설정한_크기를_넘지_않는다() {
        for (int i = 1; i <= 5; i++) {
            logService.record(entry("INFO", "메시지" + i));
        }

        assertThat(logService.recent("TRACE", 10))
                .extracting(LogEntry::message)
                .containsExactly("메시지5", "메시지4", "메시지3");
    }

    @Test
    void 레벨_필터는_지정한_레벨_이상만_돌려준다() {
        logService.record(entry("DEBUG", "디버그"));
        logService.record(entry("WARN", "경고"));
        logService.record(entry("ERROR", "에러"));

        assertThat(logService.recent("WARN", 10))
                .extracting(LogEntry::message)
                .containsExactly("에러", "경고");
    }

    @Test
    void WARN과_ERROR만_DB에_저장된다() {
        logService.record(entry("INFO", "정보"));
        logService.record(entry("WARN", "경고"));
        logService.record(entry("ERROR", "에러"));

        verify(repository, times(2)).save(any(SystemLog.class));
    }

    @Test
    void 모든_로그는_실시간_채널로_전송된다() {
        logService.record(entry("INFO", "정보"));

        verify(messagingTemplate).convertAndSend(eq(LogService.LOG_TOPIC), any(LogEntry.class));
    }

    @Test
    void 저장_중_예외가_나도_애플리케이션을_멈추지_않는다() {
        when(repository.save(any(SystemLog.class))).thenThrow(new RuntimeException("DB 장애"));

        assertThatCode(() -> logService.record(entry("ERROR", "에러"))).doesNotThrowAnyException();
        assertThat(logService.recent("TRACE", 10)).hasSize(1);
    }

    @Test
    void 로그_처리_도중_발생한_로그는_재귀하지_않는다() {
        // 저장 시점에 다시 로그가 들어오는 상황(예: DB 실패 → 에러 로그)을 흉내낸다.
        when(repository.save(any(SystemLog.class))).thenAnswer(invocation -> {
            logService.record(entry("ERROR", "저장 중 발생한 로그"));
            return invocation.getArgument(0);
        });

        logService.record(entry("ERROR", "최초 로그"));

        // 재진입이 차단되므로 버퍼에는 최초 로그만 남고 save도 한 번만 호출된다.
        assertThat(logService.recent("TRACE", 10))
                .extracting(LogEntry::message)
                .containsExactly("최초 로그");
        verify(repository, times(1)).save(any(SystemLog.class));
    }

    @Test
    void history는_WARN_미만을_요청해도_저장_대상_레벨만_조회한다() {
        logService.history("TRACE", 10);

        verify(repository).findByLevelInOrderByIdDesc(any(), any());
        verify(repository, never()).findAll();
    }
}
