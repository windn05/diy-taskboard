package com.taskboard.controller;

import com.taskboard.dto.LogDtos.LogEntry;
import com.taskboard.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 모니터링 화면의 로그 조회. 실시간 스트림은 /topic/admin/logs WebSocket으로 별도 푸시 */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final LogService logService;

    /** 메모리 버퍼의 최근 로그. 실시간 뷰어가 구독 직후 초기 화면을 채우는 용도 */
    @GetMapping
    public List<LogEntry> recent(@RequestParam(defaultValue = "TRACE") String level,
                                  @RequestParam(defaultValue = "200") int limit) {
        return logService.recent(level, limit);
    }

    /** 재시작 이후에도 남아 있는 영속 로그 (WARN/ERROR) */
    @GetMapping("/history")
    public List<LogEntry> history(@RequestParam(defaultValue = "WARN") String level,
                                   @RequestParam(defaultValue = "100") int limit) {
        return logService.history(level, limit);
    }
}
