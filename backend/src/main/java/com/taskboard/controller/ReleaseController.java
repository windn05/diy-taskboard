package com.taskboard.controller;

import com.taskboard.dto.CardDtos.CardResponse;
import com.taskboard.dto.ReleaseDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.ReleaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping("/workspaces/{workspaceId}/releases")
    public List<ReleaseResponse> list(@AuthenticationPrincipal CurrentUser user, @PathVariable Long workspaceId) {
        return releaseService.list(user, workspaceId);
    }

    /** 아직 배포되지 않은 작업 목록. statusId를 주면 그 상태만 추린다. */
    @GetMapping("/workspaces/{workspaceId}/releases/candidates")
    public List<CardResponse> candidates(@AuthenticationPrincipal CurrentUser user,
                                          @PathVariable Long workspaceId,
                                          @RequestParam(required = false) Long statusId) {
        return releaseService.candidates(user, workspaceId, statusId);
    }

    @PostMapping("/workspaces/{workspaceId}/releases")
    @ResponseStatus(HttpStatus.CREATED)
    public ReleaseResponse create(@AuthenticationPrincipal CurrentUser user, @PathVariable Long workspaceId,
                                   @Valid @RequestBody CreateReleaseRequest request) {
        return releaseService.create(user.getId(), workspaceId, request);
    }

    @PatchMapping("/releases/{releaseId}")
    public ReleaseResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable Long releaseId,
                                   @RequestBody UpdateReleaseRequest request) {
        return releaseService.update(user.getId(), releaseId, request);
    }

    @DeleteMapping("/releases/{releaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long releaseId) {
        releaseService.delete(user.getId(), releaseId);
    }
}
