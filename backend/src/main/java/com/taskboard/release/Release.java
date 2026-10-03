package com.taskboard.release;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 프로젝트별 배포(릴리즈). 버전은 프로젝트 안에서만 유일 */
@Entity
@Table(name = "releases",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "version"}),
        indexes = @Index(name = "idx_releases_workspace", columnList = "workspace_id, id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false, length = 50)
    private String version;

    /** 패치노트 */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "released_at", nullable = false)
    private LocalDateTime releasedAt;

    @PrePersist
    void onCreate() {
        if (this.releasedAt == null) this.releasedAt = LocalDateTime.now();
    }
}
