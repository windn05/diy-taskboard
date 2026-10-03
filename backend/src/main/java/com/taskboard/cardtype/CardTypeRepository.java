package com.taskboard.cardtype;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardTypeRepository extends JpaRepository<CardType, Long> {
    List<CardType> findAllByOrderByIdAsc();
}
