package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

public class CardTypeDtos {

    public record CardTypeRequest(@NotBlank String name) {
    }

    public record CardTypeResponse(Long id, String name) {
    }
}
