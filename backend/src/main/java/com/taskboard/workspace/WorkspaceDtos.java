package com.taskboard.workspace;

import jakarta.validation.constraints.NotBlank;

/** 프로젝트·멤버 요청/응답 */
public class WorkspaceDtos {

    public record CreateWorkspaceRequest(@NotBlank String name) {
    }

    /** 프로젝트 응답 (myRole: 내 프로젝트 역할, 게스트는 GUEST) */
    public record WorkspaceResponse(Long id, String name, String myRole) {
    }

    public record AdminWorkspaceResponse(Long id, String name, boolean visible) {
    }

    /** 프로젝트 수정 요청 (null인 필드는 변경하지 않음) */
    public record UpdateWorkspaceRequest(String name, Boolean visible) {
    }

    public record MemberResponse(Long userId, String username, String name, String role) {
    }

    public record AddMemberRequest(@NotBlank String username, @NotBlank String role) {
    }
}
