package com.taskboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 로그인 계정. 게스트는 저장하지 않고 토큰에만 존재 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt 해시 */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SystemRole role = SystemRole.USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** 시스템 전역 역할. 프로젝트 안에서의 역할은 WorkspaceMember.WorkspaceRole. */
    public enum SystemRole {
        ADMIN, USER, GUEST
    }
}
