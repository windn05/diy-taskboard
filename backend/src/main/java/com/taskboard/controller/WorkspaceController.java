package com.taskboard.controller;

import com.taskboard.dto.PresenceDtos.PresenceResponse;
import com.taskboard.dto.WorkspaceDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.PresenceService;
import com.taskboard.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 사용자 관점의 프로젝트 조회. 생성·멤버 관리는 AdminWorkspaceController */
@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final PresenceService presenceService;

    // 프로젝트 생성은 관리자 전용 (POST /admin/workspaces)

    /** 내가 멤버인 프로젝트. 게스트는 공개 프로젝트 중 관리자만 속한 곳을 뺀 목록 */
    @GetMapping
    public List<WorkspaceResponse> listMine(@AuthenticationPrincipal CurrentUser user) {
        return workspaceService.listMine(user);
    }

    @GetMapping("/{workspaceId}/members")
    public List<MemberResponse> listMembers(@AuthenticationPrincipal CurrentUser user, @PathVariable Long workspaceId) {
        return workspaceService.listMembers(user, workspaceId);
    }

    /** 구독 직후 초기 목록 조회용. 이후 변경은 WebSocket으로 푸시 */
    @GetMapping("/{workspaceId}/presence")
    public PresenceResponse presence(@AuthenticationPrincipal CurrentUser user, @PathVariable Long workspaceId) {
        workspaceService.requireReadAccess(user, workspaceId);
        return presenceService.snapshot(workspaceId);
    }

    // 멤버 추가/제거는 관리자 전용 (/admin/workspaces/{id}/members)
}
