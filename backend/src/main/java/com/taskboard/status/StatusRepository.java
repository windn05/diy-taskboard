package com.taskboard.status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusRepository extends JpaRepository<Status, Long> {
    List<Status> findAllByOrderByOrderAsc();
}
