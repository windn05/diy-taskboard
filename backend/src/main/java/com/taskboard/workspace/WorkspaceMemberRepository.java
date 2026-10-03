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

    /**
     * 지정한 역할이 <b>아닌</b> 멤버가 한 명이라도 있는 프로젝트 id.
     * 게스트에게 보여줄 범위를 정할 때 {@code ADMIN}을 빼고 호출 — 관리자만 있는 프로젝트는 숨김
     *
     * <p>{@code WorkspaceMember.userId}가 연관관계가 아닌 값이라 User와는 조건으로 조인
     */
    @Query("""
            select distinct m.workspaceId
            from WorkspaceMember m, User u
            where u.id = m.userId and u.role <> :excluded
            """)
    List<Long> findWorkspaceIdsWithMemberRoleOtherThan(@Param("excluded") SystemRole excluded);

    /** 한 프로젝트에 대한 같은 판단. 목록이 아니라 단건 접근을 막을 때 사용 */
    @Query("""
            select count(m) > 0
            from WorkspaceMember m, User u
            where u.id = m.userId and m.workspaceId = :workspaceId and u.role <> :excluded
            """)
    boolean existsMemberRoleOtherThan(@Param("workspaceId") Long workspaceId, @Param("excluded") SystemRole excluded);
}
