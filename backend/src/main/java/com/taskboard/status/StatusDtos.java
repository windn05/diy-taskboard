package com.taskboard.status;

import jakarta.validation.constraints.NotBlank;

/** 작업 상태 요청/응답 */
public class StatusDtos {

    public record StatusRequest(@NotBlank String name) {
    }

    public record StatusResponse(Long id, String name, Integer order) {
    }
}
