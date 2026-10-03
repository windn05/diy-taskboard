package com.taskboard.realtime;

import com.taskboard.global.security.CurrentUser;
import com.taskboard.realtime.PresenceDtos.PresenceResponse;
import com.taskboard.realtime.PresenceDtos.PresenceUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 프로젝트별 접속자를 메모리에 집계. 단일 서버 기준이며,
 * 다중 서버로 확장하면 Redis 등 공유 저장소로 이전 필요
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final Pattern PRESENCE_DESTINATION = Pattern.compile("^/topic/workspaces/(\\d+)/presence$");

    private final SimpMessagingTemplate messagingTemplate;

    /** key: STOMP 세션ID|구독ID — 한 세션이 여러 프로젝트를 구독할 수 있어 구독 단위로 관리 */
    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    private record Registration(Long workspaceId, String sessionId, PresenceUser user) {
    }

    /** 접속자 토픽(/topic/workspaces/{id}/presence)을 구독한 경우에만 프로젝트 id 반환 */
    public static Optional<Long> parseWorkspaceId(String destination) {
        if (destination == null) return Optional.empty();
        Matcher matcher = PRESENCE_DESTINATION.matcher(destination);
        return matcher.matches() ? Optional.of(Long.valueOf(matcher.group(1))) : Optional.empty();
    }

    public void join(String sessionId, String subscriptionId, Long workspaceId, CurrentUser user) {
        PresenceUser presenceUser = new PresenceUser(user.getId(), user.getUsername(), user.isGuest());
        registrations.put(key(sessionId, subscriptionId), new Registration(workspaceId, sessionId, presenceUser));
        broadcast(workspaceId);
    }

    public void leave(String sessionId, String subscriptionId) {
        Registration removed = registrations.remove(key(sessionId, subscriptionId));
        if (removed != null) broadcast(removed.workspaceId());
    }

    public void disconnect(String sessionId) {
        Set<Long> affected = new HashSet<>();
        registrations.values().removeIf(registration -> {
            if (!registration.sessionId().equals(sessionId)) return false;
            affected.add(registration.workspaceId());
            return true;
        });
        affected.forEach(this::broadcast);
    }

    /** 여러 탭으로 들어온 같은 사용자는 한 명으로 합침(distinct) */
    public PresenceResponse snapshot(Long workspaceId) {
        return new PresenceResponse(registrations.values().stream()
                .filter(registration -> registration.workspaceId().equals(workspaceId))
                .map(Registration::user)
                .distinct()
                .sorted(Comparator.comparing(PresenceUser::username))
                .toList());
    }

    private void broadcast(Long workspaceId) {
        messagingTemplate.convertAndSend("/topic/workspaces/" + workspaceId + "/presence", snapshot(workspaceId));
    }

    private String key(String sessionId, String subscriptionId) {
        return sessionId + "|" + subscriptionId;
    }
}
