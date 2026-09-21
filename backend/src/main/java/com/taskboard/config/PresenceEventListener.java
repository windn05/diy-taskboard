package com.taskboard.config;

import com.taskboard.security.CurrentUser;
import com.taskboard.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;

/**
 * STOMP 구독/해제/연결 종료 이벤트를 접속자 현황(PresenceService)에 반영.
 * 프로젝트 토픽 구독은 입장, 해제·끊김은 퇴장으로 처리
 */
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

    /** 탭을 닫는 등 UNSUBSCRIBE 없이 끊긴 경우. 세션에 딸린 구독을 한꺼번에 정리 */
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
