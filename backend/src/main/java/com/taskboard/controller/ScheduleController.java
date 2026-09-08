package com.taskboard.controller;

import com.taskboard.dto.ScheduleDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.PersonalScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final PersonalScheduleService scheduleService;

    @GetMapping
    public List<ScheduleResponse> list(@AuthenticationPrincipal CurrentUser user) {
        return scheduleService.list(user.getId());
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
