package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** 프로젝트·멤버 요청/응답 */
public class WorkspaceDtos {

    public record CreateWorkspaceRequest(@NotBlank String name) {
    }

    /** myRole: 내 프로젝트 역할(OWNER/ADMIN/MEMBER). 게스트가 볼 때는 GUEST */
    public record WorkspaceResponse(Long id, String name, Long ownerId, String myRole) {
    }

    public record AdminWorkspaceResponse(Long id, String name, Long ownerId, boolean visible) {
    }

    /** null(이름은 빈 값 포함)인 필드는 변경하지 않음 */
    public record UpdateWorkspaceRequest(String name, Boolean visible) {
    }

    public record MemberResponse(Long userId, String username, String name, String role) {
    }

    public record AddMemberRequest(@NotBlank String username, @NotBlank String role) {
    }

    public record WorkspaceListResponse(List<WorkspaceResponse> workspaces) {
    }
}
