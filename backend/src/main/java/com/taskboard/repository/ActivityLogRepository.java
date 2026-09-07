package com.taskboard.repository;

import com.taskboard.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByCardIdOrderByCreatedAtDesc(Long cardId);
    void deleteByCardIdIn(List<Long> cardIds);
}
