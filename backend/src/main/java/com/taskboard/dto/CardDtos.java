package com.taskboard.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 작업(카드) 요청/응답 */
public class CardDtos {

    /** title 외에는 모두 선택. 비우면 유형 Task, 우선순위 MEDIUM, 첫 번째 상태로 생성 */
    public record CreateCardRequest(
            @NotBlank String title,
            String description,
            String type,
            String priority,
            Long assigneeId,
            LocalDate startDate,
            LocalDate dueDate,
            Long statusId) {
    }

    /**
     * 부분 수정 요청. 담당자·시작일·마감일은 <b>값 비우기</b>가 정상적인 조작이라
     * "전달하지 않음"과 "null로 지움"의 구분 필요.
     * Jackson은 JSON에 키가 있을 때만 setter를 호출하므로, 세 필드는 setter에서 전달 여부 기록
     */
    @Getter
    @Setter
    public static class UpdateCardRequest {
        private String title;
        private String description;
        private String type;
        private String priority;
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

    /** releaseId가 있으면 배포된(완료된) 작업 */
    public record CardResponse(
            Long id, Long workspaceId, Long statusId, String title, String description, String type, String priority,
            Long assigneeId, LocalDate startDate, LocalDate dueDate, Long releaseId,
            long commentCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
