package com.taskboard.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** 개인 일정 요청/응답 */
public class ScheduleDtos {

    /** 일정 추가·수정 요청 (수정도 전체 값을 보냄) */
    public record ScheduleRequest(
            @NotBlank String title,
            @NotNull LocalDate startDate,
            @NotNull LocalDate dueDate) {
    }

    public record ScheduleResponse(Long id, String title, LocalDate startDate, LocalDate dueDate) {
    }
}
