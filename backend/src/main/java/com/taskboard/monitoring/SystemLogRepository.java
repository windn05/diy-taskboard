package com.taskboard.monitoring;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findByLevelInOrderByIdDesc(Collection<String> levels, Pageable pageable);

    /*************************************************************************
     * 목적 : 기준 시각 이전 로그를 한 번에 삭제 (벌크 DELETE)
     * 이유 : 파생 메서드(deleteByLoggedAtBefore)는 대상을 전부 읽어 한 건씩 지워, 로그가 쌓였을 때
     *        스택트레이스까지 메모리에 올라감
     * 파라미터
     * - cutoff : 이 시각 이전 로그 삭제
     * 반환
     * - 삭제된 행 수
     *************************************************************************/
    @Modifying
    @Query("delete from SystemLog l where l.loggedAt < :cutoff")
    int deleteLoggedBefore(@Param("cutoff") LocalDateTime cutoff);
}
