package com.taskboard.service;

import com.taskboard.domain.PersonalSchedule;
import com.taskboard.dto.ScheduleDtos.CreateScheduleRequest;
import com.taskboard.dto.ScheduleDtos.ScheduleResponse;
import com.taskboard.exception.AccessDeniedException;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.PersonalScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 개인 일정. 조회·삭제 모두 본인 것만 대상 */
@Service
@RequiredArgsConstructor
public class PersonalScheduleService {

    private final PersonalScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(Long userId) {
        return scheduleRepository.findByUserIdOrderByStartDateAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ScheduleResponse create(Long userId, CreateScheduleRequest request) {
        if (request.dueDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
        PersonalSchedule schedule = scheduleRepository.save(PersonalSchedule.builder()
                .userId(userId)
                .title(request.title())
                .startDate(request.startDate())
                .dueDate(request.dueDate())
                .build());
        return toResponse(schedule);
    }

    @Transactional
    public void delete(Long userId, Long scheduleId) {
        PersonalSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("일정을 찾을 수 없습니다."));
        if (!schedule.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 일정만 삭제할 수 있습니다.");
        }
        scheduleRepository.delete(schedule);
    }

    private ScheduleResponse toResponse(PersonalSchedule schedule) {
        return new ScheduleResponse(schedule.getId(), schedule.getTitle(), schedule.getStartDate(), schedule.getDueDate());
    }
}
