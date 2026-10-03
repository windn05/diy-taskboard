package com.taskboard.release;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

    List<Release> findByWorkspaceIdOrderByIdDesc(Long workspaceId);

    List<Release> findByWorkspaceIdInOrderByIdDesc(List<Long> workspaceIds);

    /*************************************************************************
     * 목적 : 프로젝트 안에서 같은 버전의 배포 조회 (중복 확인용)
     * 이유 : -
     * 파라미터
     * - workspaceId : 프로젝트 id
     * - version : 버전 이름
     * 반환
     * - 해당 배포 (없으면 빈 값)
     *************************************************************************/
    Optional<Release> findByWorkspaceIdAndVersion(Long workspaceId, String version);

    void deleteByWorkspaceId(Long workspaceId);
}
