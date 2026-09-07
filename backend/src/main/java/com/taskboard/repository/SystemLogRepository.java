package com.taskboard.repository;

import com.taskboard.domain.SystemLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findByLevelInOrderByIdDesc(Collection<String> levels, Pageable pageable);
}
