package com.taskboard.controller;

import com.taskboard.dto.StatusDtos.*;
import com.taskboard.service.StatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/statuses")
@RequiredArgsConstructor
public class AdminStatusController {

    private final StatusService statusService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StatusResponse create(@Valid @RequestBody StatusRequest request) {
        return statusService.create(request);
    }

    @PatchMapping("/{statusId}")
    public StatusResponse rename(@PathVariable Long statusId, @Valid @RequestBody StatusRequest request) {
        return statusService.rename(statusId, request);
    }

    @DeleteMapping("/{statusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long statusId) {
        statusService.delete(statusId);
    }
}
