package com.taskboard.service;

import com.taskboard.domain.PersonalSchedule;
import com.taskboard.domain.User.SystemRole;
import com.taskboard.dto.ScheduleDtos.CreateScheduleRequest;
import com.taskboard.dto.ScheduleDtos.ScheduleResponse;
import com.taskboard.exception.AccessDeniedException;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.PersonalScheduleRepository;
import com.taskboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 개인 일정. 사용자는 본인 것만 조회·삭제, 게스트는 관리자를 뺀 사용자들의 일정을 읽기 전용으로 조회 */
@Service
@RequiredArgsConstructor
public class PersonalScheduleService {

    private final PersonalScheduleRepository scheduleRepository;

    /** 게스트는 자기 일정이 없으므로 둘러보기용으로 다른 사용자들의 일정을 보여줌 */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(CurrentUser user) {
        List<PersonalSchedule> schedules = user.isGuest()
                ? scheduleRepository.findOwnedByRoleOtherThan(SystemRole.ADMIN)
                : scheduleRepository.findByUserIdOrderByStartDateAsc(user.getId());
        return schedules.stream().map(this::toResponse).toList();
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
