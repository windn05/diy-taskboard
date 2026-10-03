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

/** 배포(버전) 기록. 배포에 묶인 작업은 완료로 간주 */
@RestController
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseService releaseService;

    @GetMapping("/workspaces/{workspaceId}/releases")
    public ResponseEntity<List<ReleaseResponse>> list(@AuthenticationPrincipal CurrentUser user,
                                                      @PathVariable Long workspaceId) {
        return ResponseEntity.ok(releaseService.list(user, workspaceId));
    }

    /** 아직 배포되지 않은 작업 목록. statusId를 주면 해당 상태만 조회 */
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

    /** 묶여 있던 작업은 삭제되지 않고 미배포 상태로 복귀 */
    @DeleteMapping("/releases/{releaseId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long releaseId) {
        releaseService.delete(user.getId(), releaseId);
        return ResponseEntity.noContent().build();
    }
}
