package com.taskboard.repository;

import com.taskboard.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByCardIdOrderByCreatedAtAsc(Long cardId);
    void deleteByCardIdIn(List<Long> cardIds);
}
