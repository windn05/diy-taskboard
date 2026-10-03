package com.taskboard.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 작업 요청/응답 */
public class CardDtos {

    /** 작업 생성 요청 (title 외에는 선택) */
    public record CreateCardRequest(
            @NotBlank String title,
            String description,
            String type,
            LocalDate startDate,
            LocalDate dueDate,
            Long statusId) {
    }

    /** 작업 부분 수정 요청 (날짜는 "보내지 않음"과 "null로 비움"을 구분) */
    @Getter
    @Setter
    public static class UpdateCardRequest {
        private String title;
        private String description;
        private String type;
        private Long statusId;

        private LocalDate startDate;
        private LocalDate dueDate;

        @JsonIgnore
        private boolean startDatePresent;
        @JsonIgnore
        private boolean dueDatePresent;

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
            this.startDatePresent = true;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            this.dueDatePresent = true;
        }
    }

    /** 작업 응답 (releaseId가 있으면 배포된 작업) */
    public record CardResponse(
            Long id, Long workspaceId, Long statusId, String title, String description, String type,
            LocalDate startDate, LocalDate dueDate, Long releaseId,
            long commentCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
