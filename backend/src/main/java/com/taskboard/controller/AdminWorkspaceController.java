package com.taskboard.controller;

import com.taskboard.dto.WorkspaceDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/workspaces")
@RequiredArgsConstructor
public class AdminWorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    public List<AdminWorkspaceResponse> list() {
        return workspaceService.listAllForAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody CreateWorkspaceRequest request) {
        return workspaceService.create(user.getId(), request);
    }

    @PatchMapping("/{workspaceId}")
    public AdminWorkspaceResponse update(@PathVariable Long workspaceId, @RequestBody UpdateWorkspaceRequest request) {
        return workspaceService.updateAsAdmin(workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long workspaceId) {
        workspaceService.deleteAsAdmin(workspaceId);
    }

    @GetMapping("/{workspaceId}/members")
    public List<MemberResponse> listMembers(@PathVariable Long workspaceId) {
        return workspaceService.listMembersAsAdmin(workspaceId);
    }

    @PostMapping("/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@PathVariable Long workspaceId, @Valid @RequestBody AddMemberRequest request) {
        return workspaceService.addMemberAsAdmin(workspaceId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long workspaceId, @PathVariable Long userId) {
        workspaceService.removeMemberAsAdmin(workspaceId, userId);
    }
}
