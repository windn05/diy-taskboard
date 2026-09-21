package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** 개인 일정 요청/응답 */
public class ScheduleDtos {

    public record CreateScheduleRequest(
            @NotBlank String title,
            @NotNull LocalDate startDate,
            @NotNull LocalDate dueDate) {
    }

    public record ScheduleResponse(Long id, String title, LocalDate startDate, LocalDate dueDate) {
    }
}
