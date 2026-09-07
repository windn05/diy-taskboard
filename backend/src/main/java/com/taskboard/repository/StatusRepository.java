package com.taskboard.repository;

import com.taskboard.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusRepository extends JpaRepository<Status, Long> {
    List<Status> findAllByOrderByOrderAsc();
}
