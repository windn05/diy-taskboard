package com.taskboard.security;

import com.taskboard.domain.User.SystemRole;
import com.taskboard.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WebSocket 핸드셰이크(HttpSessionHandshakeInterceptor, WebSocketConfig)가 HTTP 세션을
 * 복사해 둔 것에서 로그인 정보를 그대로 꺼내 쓴다 — REST와 같은 세션을 공유하는 것뿐이라
 * 토큰을 따로 검증할 필요가 없다. 이후 SUBSCRIBE/DISCONNECT에서 쓸 수 있도록 세션 Principal 설정
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final WorkspaceService workspaceService;

    /** 관리자 전용 토픽 접두어. REST의 /admin/** 과 같은 기준을 WebSocket에도 적용 */
    private static final String ADMIN_TOPIC_PREFIX = "/topic/admin/";

    /** 개인 채널. /topic/users/{userId}/... 는 본인만 구독 가능 */
    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/users/(-?\\d+)/.+$");

    /** 프로젝트 채널. /topic/workspaces/{id}/... 는 그 프로젝트를 볼 수 있는 사람만 구독 가능 */
    private static final Pattern WORKSPACE_TOPIC = Pattern.compile("^/topic/workspaces/(\\d+)/.+$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(resolveFromSession(accessor.getSessionAttributes())
                    .orElseThrow(() -> new MessagingException("인증되지 않은 WebSocket 연결입니다.")));
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor.getDestination(), accessor.getUser());
        }
        return message;
    }

    /** HttpSessionHandshakeInterceptor가 복사해 둔 세션 속성에서 Spring Security의 SecurityContext를 꺼낸다 */
    private Optional<Authentication> resolveFromSession(Map<String, Object> sessionAttributes) {
        if (sessionAttributes == null) return Optional.empty();
        Object attribute = sessionAttributes.get(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return Optional.ofNullable(attribute)
                .filter(SecurityContext.class::isInstance)
                .map(SecurityContext.class::cast)
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth.getPrincipal() instanceof CurrentUser);
    }

    private void authorizeSubscribe(String destination, Principal principal) {
        if (destination == null) return;

        if (destination.startsWith(ADMIN_TOPIC_PREFIX) && !isAdmin(principal)) {
            throw new MessagingException("관리자만 구독할 수 있는 채널입니다.");
        }

        Matcher userTopic = USER_TOPIC.matcher(destination);
        if (userTopic.matches() && !isSelf(principal, Long.valueOf(userTopic.group(1)))) {
            throw new MessagingException("본인만 구독할 수 있는 채널입니다.");
        }

        Matcher workspaceTopic = WORKSPACE_TOPIC.matcher(destination);
        if (workspaceTopic.matches()) {
            requireWorkspaceReadAccess(principal, Long.valueOf(workspaceTopic.group(1)));
        }
    }

    /**
     * REST와 같은 조회 권한을 구독에도 적용. 없으면 목록에서 숨긴 프로젝트라도
     * 주소만 알면 실시간 이벤트(작업·댓글·접속자)를 그대로 받아볼 수 있음
     */
    private void requireWorkspaceReadAccess(Principal principal, Long workspaceId) {
        CurrentUser user = currentUser(principal);
        if (user == null) {
            throw new MessagingException("인증되지 않은 구독입니다.");
        }
        try {
            workspaceService.requireReadAccess(user, workspaceId);
        } catch (RuntimeException e) {
            // 존재하지 않는 프로젝트인지 권한이 없는 것인지 구분해서 알려주지 않음
            throw new MessagingException("구독할 수 없는 프로젝트입니다.");
        }
    }

    private boolean isAdmin(Principal principal) {
        CurrentUser user = currentUser(principal);
        return user != null && SystemRole.ADMIN.name().equals(user.getRole());
    }

    private boolean isSelf(Principal principal, Long userId) {
        CurrentUser user = currentUser(principal);
        return user != null && userId.equals(user.getId());
    }

    private CurrentUser currentUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof CurrentUser user) {
            return user;
        }
        return null;
    }
}
