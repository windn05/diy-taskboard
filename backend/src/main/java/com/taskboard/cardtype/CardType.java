package com.taskboard.cardtype;

import jakarta.persistence.*;
import lombok.*;

/** 작업 유형 (Task, Bug 등) */
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
