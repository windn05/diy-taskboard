package com.taskboard.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);
    /*************************************************************************
     * 목적 : 특정 상태에 속한 작업 조회
     * 이유 : 상태를 삭제하기 전 사용 중인지 확인하는 용도
     * 파라미터
     * - statusId : 상태 id
     * 반환
     * - 해당 상태의 작업 목록
     *************************************************************************/
    List<Card> findByStatusId(Long statusId);

    /*************************************************************************
     * 목적 : 다음 배포 후보가 될 미배포 작업 조회 (등록순)
     * 이유 : -
     * 파라미터
     * - workspaceId : 프로젝트 id
     * 반환
     * - 미배포 작업 목록 (등록순)
     *************************************************************************/
    List<Card> findByWorkspaceIdAndReleaseIdIsNullOrderByCreatedAtAsc(Long workspaceId);

    List<Card> findByReleaseIdInOrderByCreatedAtAsc(List<Long> releaseIds);

    /*************************************************************************
     * 목적 : 여러 프로젝트의 작업을 한 번에 조회
     * 이유 : 대시보드에서 프로젝트마다 따로 조회하면 쿼리가 프로젝트 수만큼 늘어남
     * 파라미터
     * - workspaceIds : 프로젝트 id 목록
     * 반환
     * - 작업 목록
     *************************************************************************/
    List<Card> findByWorkspaceIdIn(List<Long> workspaceIds);
}
