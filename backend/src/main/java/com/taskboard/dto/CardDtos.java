package com.taskboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CardDtos {

    public record CreateCardRequest(
            @NotBlank String title,
            String description,
            String type,
            String priority,
            Long assigneeId,
            List<String> labels,
            LocalDate startDate,
            LocalDate dueDate,
            Long statusId) {
    }

    /**
     * 부분 수정 요청. 담당자·시작일·마감일은 <b>값을 비우는 것</b>이 정상적인 조작이라
     * "전달하지 않음"과 "null로 지움"을 구분해야 한다.
     * Jackson은 JSON에 키가 있을 때만 setter를 호출하므로, 그 세 필드는 setter에서 전달 여부를 기록한다.
     */
    @Getter
    @Setter
    public static class UpdateCardRequest {
        private String title;
        private String description;
        private String type;
        private String priority;
        private List<String> labels;
        private Long statusId;

        private Long assigneeId;
        private LocalDate startDate;
        private LocalDate dueDate;

        @JsonIgnore
        private boolean assigneeIdPresent;
        @JsonIgnore
        private boolean startDatePresent;
        @JsonIgnore
        private boolean dueDatePresent;

        public void setAssigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;
            this.assigneeIdPresent = true;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
            this.startDatePresent = true;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            this.dueDatePresent = true;
        }
    }

    public record CardResponse(
            Long id, Long workspaceId, Long statusId, String title, String description, String type, String priority,
            Long assigneeId, List<String> labels, LocalDate startDate, LocalDate dueDate, Long releaseId,
            long commentCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
