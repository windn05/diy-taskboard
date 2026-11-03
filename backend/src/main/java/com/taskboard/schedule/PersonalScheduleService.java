package com.taskboard.schedule;

import com.taskboard.global.exception.AccessDeniedException;
import com.taskboard.global.exception.EntityNotFoundException;
import com.taskboard.global.security.CurrentUser;
import com.taskboard.schedule.ScheduleDtos.ScheduleRequest;
import com.taskboard.schedule.ScheduleDtos.ScheduleResponse;
import com.taskboard.user.User.SystemRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 개인 일정 관리 */
@Service
@RequiredArgsConstructor
public class PersonalScheduleService {

    private final PersonalScheduleRepository scheduleRepository;

    /*************************************************************************
     * 목적 : 일정 목록 조회 (사용자는 본인 것, 게스트는 관리자를 뺀 사용자들 것)
     * 이유 : 게스트는 자기 일정이 없어 그대로면 달력이 비어 보임
     * 파라미터
     * - user : 로그인 사용자
     * 반환
     * - 일정 목록 (시작일순)
     *************************************************************************/
    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(CurrentUser user) {
        List<PersonalSchedule> schedules = user.isGuest()
                ? scheduleRepository.findOwnedByRoleOtherThan(SystemRole.ADMIN)
                : scheduleRepository.findByUserIdOrderByStartDateAsc(user.getId());
        return schedules.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ScheduleResponse create(Long userId, ScheduleRequest request) {
        validateDates(request);
        PersonalSchedule schedule = scheduleRepository.save(PersonalSchedule.builder()
                .userId(userId)
                .title(request.title())
                .startDate(request.startDate())
                .dueDate(request.dueDate())
                .build());
        return toResponse(schedule);
    }

    /*************************************************************************
     * 목적 : 본인 일정의 제목·기간 수정
     * 이유 : -
     * 파라미터
     * - userId : 수정하는 사용자 id
     * - scheduleId : 일정 id
     * - request : 바꿀 제목·시작일·종료일 (전체 값)
     * 반환
     * - 수정된 일정
     *************************************************************************/
    @Transactional
    public ScheduleResponse update(Long userId, Long scheduleId, ScheduleRequest request) {
        validateDates(request);
        PersonalSchedule schedule = getOwned(userId, scheduleId, "본인 일정만 수정할 수 있습니다.");
        schedule.setTitle(request.title());
        schedule.setStartDate(request.startDate());
        schedule.setDueDate(request.dueDate());
        return toResponse(schedule);
    }

    @Transactional
    public void delete(Long userId, Long scheduleId) {
        scheduleRepository.delete(getOwned(userId, scheduleId, "본인 일정만 삭제할 수 있습니다."));
    }

    private void validateDates(ScheduleRequest request) {
        if (request.dueDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    private PersonalSchedule getOwned(Long userId, Long scheduleId, String deniedMessage) {
        PersonalSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("일정을 찾을 수 없습니다."));
        if (!schedule.getUserId().equals(userId)) {
            throw new AccessDeniedException(deniedMessage);
        }
        return schedule;
    }

    private ScheduleResponse toResponse(PersonalSchedule schedule) {
        return new ScheduleResponse(schedule.getId(), schedule.getTitle(), schedule.getStartDate(), schedule.getDueDate());
    }
}
