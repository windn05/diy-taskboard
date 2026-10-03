package com.taskboard.workspace;

import com.taskboard.global.security.CurrentUser;
import com.taskboard.realtime.PresenceDtos.PresenceResponse;
import com.taskboard.realtime.PresenceService;
import com.taskboard.workspace.WorkspaceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 사용자용 프로젝트 조회 API */
@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final PresenceService presenceService;

    // 프로젝트 생성은 관리자 전용 (POST /admin/workspaces)

    /*************************************************************************
     * 목적 : 내가 볼 수 있는 프로젝트 조회 (게스트는 공개 프로젝트 중 관리자만 속한 곳 제외)
     * 이유 : -
     * 파라미터
     * - user : 로그인 사용자
     * 반환
     * - 200 + 프로젝트 목록
     *************************************************************************/
    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> listMine(@AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(workspaceService.listMine(user));
    }

    /*************************************************************************
     * 목적 : 프로젝트 접속자 초기 목록 조회
     * 이유 : 구독이 등록되는 순간의 전송은 놓칠 수 있어 현재 목록을 한 번 받아옴
     * 파라미터
     * - user : 로그인 사용자
     * - workspaceId : 프로젝트 id
     * 반환
     * - 200 + 접속자 목록
     *************************************************************************/
    @GetMapping("/{workspaceId}/presence")
    public ResponseEntity<PresenceResponse> presence(@AuthenticationPrincipal CurrentUser user,
                                                     @PathVariable Long workspaceId) {
        workspaceService.requireReadAccess(user, workspaceId);
        return ResponseEntity.ok(presenceService.snapshot(workspaceId));
    }

    // 멤버 추가/제거는 관리자 전용 (/admin/workspaces/{id}/members)
}
