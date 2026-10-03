package com.taskboard.status;

import jakarta.persistence.*;
import lombok.*;

/** 작업 상태(칸반 컬럼). 모든 프로젝트가 공유하는 전역 설정 */
@Entity
@Table(name = "statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** 컬럼 표시 순서. {@code order}는 SQL 예약어라 컬럼명 변경 */
    @Column(name = "status_order", nullable = false)
    private Integer order;
}
