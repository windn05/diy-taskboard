package com.taskboard.repository;

import com.taskboard.domain.PersonalSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalScheduleRepository extends JpaRepository<PersonalSchedule, Long> {

    List<PersonalSchedule> findByUserIdOrderByStartDateAsc(Long userId);
}
