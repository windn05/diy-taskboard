package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

/** 작업 유형 요청/응답 */
public class CardTypeDtos {

    public record CardTypeRequest(@NotBlank String name) {
    }

    public record CardTypeResponse(Long id, String name) {
    }
}
