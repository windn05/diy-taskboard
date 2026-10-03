package com.taskboard.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);
    /** 상태 삭제 전 사용 중인지 확인할 때 */
    List<Card> findByStatusId(Long statusId);

    /** 아직 어떤 배포에도 포함되지 않은 작업 — 다음 배포 후보 */
    List<Card> findByWorkspaceIdAndReleaseIdIsNullOrderByCreatedAtAsc(Long workspaceId);

    List<Card> findByReleaseIdInOrderByCreatedAtAsc(List<Long> releaseIds);

    /** 대시보드에서 여러 프로젝트의 작업을 한 번에 모을 때 */
    List<Card> findByWorkspaceIdIn(List<Long> workspaceIds);
}
