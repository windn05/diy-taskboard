package com.taskboard.service;

import com.taskboard.dto.PresenceDtos.PresenceUser;
import com.taskboard.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PresenceServiceTest {

    private PresenceService presenceService;

    private static final CurrentUser HONG = new CurrentUser(1L, "hong", "USER");
    private static final CurrentUser GUEST = new CurrentUser(-42L, "guest-a1b2", "GUEST");

    @BeforeEach
    void setUp() {
        presenceService = new PresenceService(mock(SimpMessagingTemplate.class));
    }

    @Test
    void presence_destination에서_workspaceId를_뽑는다() {
        assertThat(PresenceService.parseWorkspaceId("/topic/workspaces/7/presence")).contains(7L);
    }

    @Test
    void presence가_아닌_destination은_무시한다() {
        assertThat(PresenceService.parseWorkspaceId("/topic/workspaces/7/cards")).isEmpty();
        assertThat(PresenceService.parseWorkspaceId("/topic/admin/logs")).isEmpty();
        assertThat(PresenceService.parseWorkspaceId(null)).isEmpty();
    }

    @Test
    void 구독하면_접속자_목록에_들어간다() {
        presenceService.join("session-1", "sub-1", 1L, HONG);

        assertThat(usernames(1L)).containsExactly("hong");
    }

    @Test
    void 같은_사용자가_여러_탭을_열어도_한_번만_노출된다() {
        presenceService.join("session-1", "sub-1", 1L, HONG);
        presenceService.join("session-2", "sub-1", 1L, HONG);

        assertThat(usernames(1L)).containsExactly("hong");
    }

    @Test
    void 프로젝트별로_접속자가_분리된다() {
        presenceService.join("session-1", "sub-1", 1L, HONG);
        presenceService.join("session-2", "sub-1", 2L, GUEST);

        assertThat(usernames(1L)).containsExactly("hong");
        assertThat(usernames(2L)).containsExactly("guest-a1b2");
    }

    @Test
    void 구독을_해제하면_목록에서_빠진다() {
        presenceService.join("session-1", "sub-1", 1L, HONG);
        presenceService.leave("session-1", "sub-1");

        assertThat(usernames(1L)).isEmpty();
    }

    @Test
    void 연결이_끊기면_그_세션의_모든_구독이_제거된다() {
        presenceService.join("session-1", "sub-1", 1L, HONG);
        presenceService.join("session-1", "sub-2", 2L, HONG);
        presenceService.join("session-2", "sub-1", 1L, GUEST);

        presenceService.disconnect("session-1");

        assertThat(usernames(1L)).containsExactly("guest-a1b2");
        assertThat(usernames(2L)).isEmpty();
    }

    @Test
    void 게스트는_guest_플래그로_구분된다() {
        presenceService.join("session-1", "sub-1", 1L, GUEST);

        assertThat(presenceService.snapshot(1L).users())
                .singleElement()
                .extracting(PresenceUser::guest, PresenceUser::userId)
                .containsExactly(true, -42L);
    }

    @Test
    void 없는_구독을_해제해도_문제없다() {
        presenceService.leave("session-없음", "sub-없음");

        assertThat(presenceService.snapshot(1L).users()).isEmpty();
    }

    private List<String> usernames(Long workspaceId) {
        return presenceService.snapshot(workspaceId).users().stream().map(PresenceUser::username).toList();
    }
}
