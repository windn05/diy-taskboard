package com.taskboard.monitoring;

import com.taskboard.monitoring.LogDtos.LogEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 모니터링 로그 조회 API */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final LogService logService;

    /*************************************************************************
     * 목적 : 메모리 버퍼의 최근 로그 조회
     * 이유 : 실시간 로그 화면이 구독 직후 초기 내용을 채우는 용도
     * 파라미터
     * - level : 최소 레벨 (기본 TRACE)
     * - limit : 최대 건수 (기본 200)
     * 반환
     * - 200 + 로그 목록 (최신순)
     *************************************************************************/
    @GetMapping
    public ResponseEntity<List<LogEntry>> recent(@RequestParam(defaultValue = "TRACE") String level,
                                                 @RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(logService.recent(level, limit));
    }

    /*************************************************************************
     * 목적 : DB에 저장된 WARN/ERROR 로그 조회
     * 이유 : 메모리 로그는 재시작하면 사라지므로 이전 오류는 DB에서 확인
     * 파라미터
     * - level : 최소 레벨 (기본 WARN)
     * - limit : 최대 건수 (기본 100)
     * 반환
     * - 200 + 로그 목록 (최신순)
     *************************************************************************/
    @GetMapping("/history")
    public ResponseEntity<List<LogEntry>> history(@RequestParam(defaultValue = "WARN") String level,
                                                  @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(logService.history(level, limit));
    }
}
