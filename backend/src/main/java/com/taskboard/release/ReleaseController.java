package com.taskboard.release;

import com.taskboard.card.CardDtos.CardResponse;
import com.taskboard.global.security.CurrentUser;
import com.taskboard.release.ReleaseDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 배포 기록 API */
@RestController
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping("/workspaces/{workspaceId}/releases")
    public ResponseEntity<List<ReleaseResponse>> list(@AuthenticationPrincipal CurrentUser user,
                                                      @PathVariable Long workspaceId) {
        return ResponseEntity.ok(releaseService.list(user, workspaceId));
    }

    /*************************************************************************
     * 목적 : 아직 배포되지 않은 작업 목록 조회 (상태로 거르기 가능)
     * 이유 : -
     * 파라미터
     * - user : 로그인 사용자
     * - workspaceId : 프로젝트 id
     * - statusId : 이 상태만 조회 (선택)
     * 반환
     * - 200 + 배포 후보 작업 목록
     *************************************************************************/
    @GetMapping("/workspaces/{workspaceId}/releases/candidates")
    public ResponseEntity<List<CardResponse>> candidates(@AuthenticationPrincipal CurrentUser user,
                                                         @PathVariable Long workspaceId,
                                                         @RequestParam(required = false) Long statusId) {
        return ResponseEntity.ok(releaseService.candidates(user, workspaceId, statusId));
    }

    @PostMapping("/workspaces/{workspaceId}/releases")
    public ResponseEntity<ReleaseResponse> create(@AuthenticationPrincipal CurrentUser user,
                                                  @PathVariable Long workspaceId,
                                                  @Valid @RequestBody CreateReleaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(releaseService.create(user.getId(), workspaceId, request));
    }

    @PatchMapping("/releases/{releaseId}")
    public ResponseEntity<ReleaseResponse> update(@AuthenticationPrincipal CurrentUser user,
                                                  @PathVariable Long releaseId,
                                                  @RequestBody UpdateReleaseRequest request) {
        return ResponseEntity.ok(releaseService.update(user.getId(), releaseId, request));
    }

    /*************************************************************************
     * 목적 : 배포 취소 (묶여 있던 작업은 미배포 상태로 복귀)
     * 이유 : -
     * 파라미터
     * - user : 로그인 사용자
     * - releaseId : 배포 id
     * 반환
     * - 204 (본문 없음)
     *************************************************************************/
    @DeleteMapping("/releases/{releaseId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long releaseId) {
        releaseService.delete(user.getId(), releaseId);
        return ResponseEntity.noContent().build();
    }
}
