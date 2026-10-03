package com.taskboard.workspace;

import jakarta.persistence.*;
import lombok.*;

/** 프로젝트 멤버십 */
@Entity
@Table(name = "workspace_members", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;

    public enum WorkspaceRole {
        OWNER, ADMIN, MEMBER
    }
}
