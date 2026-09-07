package com.taskboard.security;

import com.taskboard.domain.User.SystemRole;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP CONNECT 헤더의 JWT를 REST와 동일한 방식으로 검증하고,
 * 이후 SUBSCRIBE/DISCONNECT 이벤트에서 사용자를 식별할 수 있도록 세션 Principal을 설정한다.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final TokenAuthenticator tokenAuthenticator;

    /** 관리자 전용 토픽 접두어. REST의 /admin/** 과 같은 기준을 WebSocket에도 적용한다. */
    private static final String ADMIN_TOPIC_PREFIX = "/topic/admin/";

    /** 개인 채널. /topic/users/{userId}/... 는 본인만 구독할 수 있다. */
    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/users/(-?\\d+)/.+$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            CurrentUser user = tokenAuthenticator.resolveBearer(accessor.getFirstNativeHeader("Authorization"))
                    .orElseThrow(() -> new MessagingException("인증되지 않은 WebSocket 연결입니다."));
            accessor.setUser(user.toAuthentication());
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscribe(accessor.getDestination(), accessor.getUser());
        }
        return message;
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
