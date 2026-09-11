package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public class ReleaseDtos {

    /** 배포에 포함된 작업의 요약 (패치노트 화면에 쓸 최소 정보) */
    public record ReleasedCard(Long id, String title, String type, String priority) {
    }

    public record ReleaseResponse(
            Long id,
            Long workspaceId,
            String version,
            String notes,
            LocalDateTime releasedAt,
            List<ReleasedCard> cards) {
    }

    /**
     * completedStatusId는 배포 후 작업을 옮길 상태. 화면에서 고른 값을 그대로 받는다 —
     * 상태 이름("배포 완료")은 관리자가 바꿀 수 있으므로 서버가 이름으로 추측하지 않는다.
     */
    public record CreateReleaseRequest(
            @NotBlank String version,
            String notes,
            @NotEmpty List<Long> cardIds,
            Long completedStatusId) {
    }

    /** addCardIds/completedStatusId는 이미 확정된 배포에 나중에 작업을 더 끼워 넣을 때 쓴다. */
    public record UpdateReleaseRequest(String version, String notes, List<Long> addCardIds, Long completedStatusId) {
    }
}
