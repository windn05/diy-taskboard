package com.taskboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 작업(칸반 카드).
 * 다른 엔티티는 {@code @ManyToOne} 대신 id 값으로 참조 — 지연 로딩·N+1 걱정 없이
 * 필요한 것만 명시적으로 조회하기 위함. 모든 엔티티에 공통인 규칙
 */
@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 작업 유형 이름. CardType id가 아니라 이름을 그대로 저장 */
    @Column(nullable = false)
    @Builder.Default
    private String type = "Task";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Column(name = "assignee_id")
    private Long assigneeId;

    /** 이 작업이 포함된 배포. 비어 있으면 미배포 */
    @Column(name = "release_id")
    private Long releaseId;

    @ElementCollection
    @CollectionTable(name = "card_labels", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "label")
    @Builder.Default
    private List<String> labels = new ArrayList<>();

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
}
