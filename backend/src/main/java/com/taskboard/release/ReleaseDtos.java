package com.taskboard.release;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

/** 배포 요청/응답 */
public class ReleaseDtos {

    /** 배포에 포함된 작업 요약 */
    public record ReleasedCard(Long id, String title, String type) {
    }

    public record ReleaseResponse(
            Long id,
            Long workspaceId,
            String version,
            String notes,
            LocalDateTime releasedAt,
            List<ReleasedCard> cards) {
    }

    /** 배포 생성 요청 (completedStatusId: 배포 후 작업을 옮길 상태) */
    public record CreateReleaseRequest(
            @NotBlank String version,
            String notes,
            @NotEmpty List<Long> cardIds,
            Long completedStatusId) {
    }

    /** 배포 수정 요청 (addCardIds로 작업 추가) */
    public record UpdateReleaseRequest(String version, String notes, List<Long> addCardIds, Long completedStatusId) {
    }
}
