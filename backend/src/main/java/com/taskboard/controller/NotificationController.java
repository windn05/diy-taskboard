package com.taskboard.controller;

import com.taskboard.dto.NotificationDtos.NotificationListResponse;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationListResponse list(@AuthenticationPrincipal CurrentUser user) {
        return notificationService.list(user.getId());
    }

    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal CurrentUser user, @PathVariable Long notificationId) {
        notificationService.markRead(user.getId(), notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal CurrentUser user) {
        notificationService.markAllRead(user.getId());
    }
}
