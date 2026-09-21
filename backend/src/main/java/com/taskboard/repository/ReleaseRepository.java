package com.taskboard.repository;

import com.taskboard.domain.Release;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReleaseRepository extends JpaRepository<Release, Long> {

    List<Release> findByWorkspaceIdOrderByIdDesc(Long workspaceId);

    List<Release> findByWorkspaceIdInOrderByIdDesc(List<Long> workspaceIds);

    /** 버전 중복 확인용. 버전은 프로젝트 안에서만 유일 */
    Optional<Release> findByWorkspaceIdAndVersion(Long workspaceId, String version);

    void deleteByWorkspaceId(Long workspaceId);
}
