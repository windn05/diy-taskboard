package com.taskboard.schedule;

import com.taskboard.user.User.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonalScheduleRepository extends JpaRepository<PersonalSchedule, Long> {

    List<PersonalSchedule> findByUserIdOrderByStartDateAsc(Long userId);

    /*************************************************************************
     * 목적 : 지정한 역할이 아닌 사용자들의 일정 조회 (게스트 화면용)
     * 이유 : ADMIN을 빼고 불러 관리자 개인 일정은 노출하지 않음 — 관리자만 있는 프로젝트를 숨기는 규칙과 동일
     * 파라미터
     * - excluded : 제외할 시스템 역할
     * 반환
     * - 일정 목록 (시작일순)
     *************************************************************************/
    @Query("""
            select s
            from PersonalSchedule s, User u
            where u.id = s.userId and u.role <> :excluded
            order by s.startDate
            """)
    List<PersonalSchedule> findOwnedByRoleOtherThan(@Param("excluded") SystemRole excluded);
}
