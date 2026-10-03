package com.taskboard.workspace;

import com.taskboard.global.security.CurrentUser;
import com.taskboard.workspace.WorkspaceDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 프로젝트·멤버 관리 API (관리자 전용) */
@RestController
@RequestMapping("/admin/workspaces")
@RequiredArgsConstructor
public class AdminWorkspaceController {

    private final WorkspaceService workspaceService;

    /*************************************************************************
     * 목적 : 전체 프로젝트 조회 (멤버가 아니거나 비공개인 프로젝트 포함)
     * 이유 : -
     * 파라미터
     * -
     * 반환
     * - 200 + 프로젝트 목록
     *************************************************************************/
    @GetMapping
    public ResponseEntity<List<AdminWorkspaceResponse>> list() {
        return ResponseEntity.ok(workspaceService.listAllForAdmin());
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                    @Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.create(user.getId(), request));
    }

    @PatchMapping("/{workspaceId}")
    public ResponseEntity<AdminWorkspaceResponse> update(@PathVariable Long workspaceId,
                                                         @RequestBody UpdateWorkspaceRequest request) {
        return ResponseEntity.ok(workspaceService.updateAsAdmin(workspaceId, request));
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> delete(@PathVariable Long workspaceId) {
        workspaceService.deleteAsAdmin(workspaceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<MemberResponse>> listMembers(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(workspaceService.listMembersAsAdmin(workspaceId));
    }

    @PostMapping("/{workspaceId}/members")
    public ResponseEntity<MemberResponse> addMember(@PathVariable Long workspaceId,
                                                    @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMemberAsAdmin(workspaceId, request));
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long workspaceId, @PathVariable Long userId) {
        workspaceService.removeMemberAsAdmin(workspaceId, userId);
        return ResponseEntity.noContent().build();
    }
}
