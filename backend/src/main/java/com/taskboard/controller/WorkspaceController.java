package com.taskboard.controller;

import com.taskboard.dto.PresenceDtos.PresenceResponse;
import com.taskboard.dto.WorkspaceDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.PresenceService;
import com.taskboard.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final PresenceService presenceService;

    // 프로젝트 생성은 관리자 전용이다 (POST /admin/workspaces).

    @GetMapping
    public List<WorkspaceResponse> listMine(@AuthenticationPrincipal CurrentUser user) {
        return workspaceService.listMine(user);
    }

    @GetMapping("/{workspaceId}/members")
    public List<MemberResponse> listMembers(@AuthenticationPrincipal CurrentUser user, @PathVariable Long workspaceId) {
        return workspaceService.listMembers(user, workspaceId);
    }

    /** 구독 직후 초기 목록을 받기 위한 시드용. 이후 변경은 WebSocket으로 푸시된다. */
    @GetMapping("/{workspaceId}/presence")
    public PresenceResponse presence(@AuthenticationPrincipal CurrentUser user, @PathVariable Long workspaceId) {
        workspaceService.requireReadAccess(user, workspaceId);
        return presenceService.snapshot(workspaceId);
    }

    // 멤버 추가/제거는 관리자 전용이다 (/admin/workspaces/{id}/members).
}
