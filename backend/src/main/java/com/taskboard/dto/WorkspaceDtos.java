package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class WorkspaceDtos {

    public record CreateWorkspaceRequest(@NotBlank String name) {
    }

    public record WorkspaceResponse(Long id, String name, Long ownerId, String myRole) {
    }

    public record AdminWorkspaceResponse(Long id, String name, Long ownerId, boolean visible) {
    }

    public record UpdateWorkspaceRequest(String name, Boolean visible) {
    }

    public record MemberResponse(Long userId, String username, String name, String role) {
    }

    public record AddMemberRequest(@NotBlank String username, @NotBlank String role) {
    }

    public record WorkspaceListResponse(List<WorkspaceResponse> workspaces) {
    }
}
