package com.taskboard.repository;

import com.taskboard.domain.SystemLog;
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

    /**
     * 보존 기간이 지난 로그를 벌크 DELETE로 삭제.
     * 파생 메서드(deleteByLoggedAtBefore)는 대상을 전부 엔티티로 읽어 한 건씩 지우므로,
     * 로그가 쌓였을 때 스택트레이스까지 힙에 올라감
     */
    @Modifying
    @Query("delete from SystemLog l where l.loggedAt < :cutoff")
    int deleteLoggedBefore(@Param("cutoff") LocalDateTime cutoff);
}
