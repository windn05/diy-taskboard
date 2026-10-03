package com.taskboard.workspace;

import com.taskboard.user.User.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);
    List<WorkspaceMember> findByUserId(Long userId);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    void deleteByWorkspaceId(Long workspaceId);

    /*************************************************************************
     * 목적 : 지정한 역할이 아닌 멤버가 한 명이라도 있는 프로젝트 id 조회 (게스트 공개 범위 계산용)
     * 이유 : ADMIN을 빼고 불러 관리자만 있는 프로젝트를 숨김. userId가 연관관계가 아닌 값이라 User와는
     *        조건으로 조인
     * 파라미터
     * - excluded : 제외할 시스템 역할
     * 반환
     * - 프로젝트 id 목록
     *************************************************************************/
    @Query("""
            select distinct m.workspaceId
            from WorkspaceMember m, User u
            where u.id = m.userId and u.role <> :excluded
            """)
    List<Long> findWorkspaceIdsWithMemberRoleOtherThan(@Param("excluded") SystemRole excluded);

    /*************************************************************************
     * 목적 : 한 프로젝트에 지정한 역할이 아닌 멤버가 있는지 확인
     * 이유 : 목록에서 숨긴 프로젝트에 주소로 직접 들어오는 것을 막기 위한 단건 확인
     * 파라미터
     * - workspaceId : 프로젝트 id
     * - excluded : 제외할 시스템 역할
     * 반환
     * - 있으면 true
     *************************************************************************/
    @Query("""
            select count(m) > 0
            from WorkspaceMember m, User u
            where u.id = m.userId and m.workspaceId = :workspaceId and u.role <> :excluded
            """)
    boolean existsMemberRoleOtherThan(@Param("workspaceId") Long workspaceId, @Param("excluded") SystemRole excluded);
}
