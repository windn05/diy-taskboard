package com.taskboard.schedule;

import com.taskboard.global.security.CurrentUser;
import com.taskboard.schedule.ScheduleDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 개인 일정 API */
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final PersonalScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> list(@AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(scheduleService.list(user));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                   @Valid @RequestBody CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleService.create(user.getId(), request));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long scheduleId) {
        scheduleService.delete(user.getId(), scheduleId);
        return ResponseEntity.noContent().build();
    }
}
