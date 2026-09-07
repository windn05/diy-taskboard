package com.taskboard.repository;

import com.taskboard.domain.CardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardTypeRepository extends JpaRepository<CardType, Long> {
    List<CardType> findAllByOrderByIdAsc();
}
