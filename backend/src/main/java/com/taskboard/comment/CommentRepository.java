package com.taskboard.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByCardIdOrderByCreatedAtAsc(Long cardId);
    List<Comment> findByCardIdIn(List<Long> cardIds);
    long countByCardId(Long cardId);
    void deleteByCardIdIn(List<Long> cardIds);
}
