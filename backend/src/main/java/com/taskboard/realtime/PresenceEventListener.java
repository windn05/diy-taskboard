package com.taskboard.realtime;

import com.taskboard.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

/** STOMP 구독·해제·연결 종료를 접속자 현황에 반영 */
@Component
@RequiredArgsConstructor
public class PresenceEventListener {

    private final PresenceService presenceService;

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        CurrentUser user = currentUser(event.getUser());
        if (user == null) return;
        PresenceService.parseWorkspaceId(accessor.getDestination()).ifPresent(workspaceId ->
                presenceService.join(accessor.getSessionId(), accessor.getSubscriptionId(), workspaceId, user));
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        presenceService.leave(accessor.getSessionId(), accessor.getSubscriptionId());
    }

    /*************************************************************************
     * 목적 : 연결이 끊기면 그 세션의 구독을 한꺼번에 정리
     * 이유 : 탭을 닫는 등 UNSUBSCRIBE 없이 끊기는 경우에도 퇴장 처리가 되도록 함
     * 파라미터
     * - event : 연결 종료 이벤트
     * 반환
     * -
     *************************************************************************/
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        presenceService.disconnect(event.getSessionId());
    }

    private CurrentUser currentUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof CurrentUser user) {
            return user;
        }
        return null;
    }
}
