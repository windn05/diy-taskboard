package com.taskboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자별 알림. 작업 제목·작성자 이름은 발생 시점의 값을 그대로 저장.
 * 알림은 "그때 일어난 일"이라, 나중에 작업 제목이 바뀌어도 당시 맥락을 남기기 위함
 */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notifications_user", columnList = "user_id, id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받는 사람 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "card_title", nullable = false)
    private String cardTitle;

    /** 알림을 발생시킨 사람의 이름 */
    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Type {
        COMMENT
    }
}
