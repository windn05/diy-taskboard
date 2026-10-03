package com.taskboard.realtime;

import com.taskboard.global.security.CurrentUser;
import com.taskboard.user.User.SystemRole;
import com.taskboard.workspace.WorkspaceService;
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

/** STOMP 연결 시 세션의 로그인 정보 연결, 구독 시 권한 검사 */
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

    /*************************************************************************
     * 목적 : WebSocket 세션 속성에서 로그인 정보 추출
     * 이유 : 핸드셰이크 때 HTTP 세션 속성이 복사되므로 REST와 같은 로그인 정보를 그대로 사용
     * 파라미터
     * - sessionAttributes : 핸드셰이크 때 복사된 세션 속성
     * 반환
     * - 인증 정보 (비로그인이면 빈 값)
     *************************************************************************/
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

    /*************************************************************************
     * 목적 : 프로젝트 토픽 구독 권한 검사 (REST 조회 권한과 동일)
     * 이유 : 없으면 목록에서 숨긴 프로젝트라도 주소만 알면 실시간 이벤트를 받아볼 수 있음
     * 파라미터
     * - principal : 구독한 사용자
     * - workspaceId : 프로젝트 id
     * 반환
     * -
     *************************************************************************/
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
