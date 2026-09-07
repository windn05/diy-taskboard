package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

public class StatusDtos {

    public record StatusRequest(@NotBlank String name) {
    }

    public record StatusResponse(Long id, String name, Integer order) {
    }
}
