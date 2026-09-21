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
     * 보존 기간이 지난 로그를 지운다.
     *
     * <p>파생 메서드 {@code deleteByLoggedAtBefore}를 쓰지 않은 이유: 그쪽은 대상 행을 전부 엔티티로
     * 읽어들인 뒤 한 건씩 지운다. 정리가 밀려 테이블이 커졌을 때가 바로 이 메서드가 필요한 순간인데,
     * 그때 스택트레이스까지 통째로 힙(-Xmx320m)에 올리게 된다. 벌크 DELETE 한 방이면 그 일이 없다.
     */
    @Modifying
    @Query("delete from SystemLog l where l.loggedAt < :cutoff")
    int deleteLoggedBefore(@Param("cutoff") LocalDateTime cutoff);
}
