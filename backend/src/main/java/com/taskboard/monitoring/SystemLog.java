package com.taskboard.monitoring;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** DB에 저장되는 시스템 로그 (WARN/ERROR만) */
@Entity
@Table(name = "system_logs", indexes = @Index(name = "idx_system_logs_level_id", columnList = "level, id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(name = "logger_name", nullable = false)
    private String loggerName;

    @Column(name = "thread_name", nullable = false)
    private String threadName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;
}
