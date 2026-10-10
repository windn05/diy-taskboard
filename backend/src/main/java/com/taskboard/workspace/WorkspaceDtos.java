package com.taskboard.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 프로젝트·멤버 요청/응답 */
public class WorkspaceDtos {

    public record CreateWorkspaceRequest(@NotBlank String name) {
    }

    /** 프로젝트 응답 (myRole: 내 프로젝트 역할, 게스트는 GUEST / groupName: 사이드바 분류, 없으면 null) */
    public record WorkspaceResponse(Long id, String name, String myRole, String groupName) {
    }

    public record AdminWorkspaceResponse(Long id, String name, boolean visible, String groupName) {
    }

    /** 프로젝트 수정 요청 (null인 필드는 변경하지 않음, groupName은 빈 문자열이면 그룹 해제) */
    public record UpdateWorkspaceRequest(String name, Boolean visible, @Size(max = 50) String groupName) {
    }

    public record MemberResponse(Long userId, String username, String name, String role) {
    }

    public record AddMemberRequest(@NotBlank String username, @NotBlank String role) {
    }
}
