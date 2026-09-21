package com.taskboard.domain;

import jakarta.persistence.*;
import lombok.*;

/** 작업 유형(Task/Bug/Story 등). 관리자가 편집하는 전역 설정 */
@Entity
@Table(name = "card_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
}
