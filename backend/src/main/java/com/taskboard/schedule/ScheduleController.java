package com.taskboard.schedule;

import com.taskboard.global.security.CurrentUser;
import com.taskboard.schedule.ScheduleDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 개인 일정. 프로젝트와 무관. 사용자는 본인 것만, 게스트는 관리자를 뺀 사용자들의 것을 읽기 전용으로 조회 */
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final PersonalScheduleService scheduleService;

    @GetMapping
    public List<ScheduleResponse> list(@AuthenticationPrincipal CurrentUser user) {
        return scheduleService.list(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponse create(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody CreateScheduleRequest request) {
        return scheduleService.create(user.getId(), request);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long scheduleId) {
        scheduleService.delete(user.getId(), scheduleId);
    }
}
