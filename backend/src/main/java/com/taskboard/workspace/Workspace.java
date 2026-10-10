package com.taskboard.workspace;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 프로젝트 */
@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long ownerId;

    /** 게스트 공개 여부. 멤버는 이 값과 무관하게 자기 프로젝트 조회 가능 */
    @Column(nullable = false)
    @Builder.Default
    private boolean visible = true;

    /** 사이드바 분류 그룹. 없으면 "기타"로 표시 */
    @Column(length = 50)
    private String groupName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
