package com.taskboard.service;

import com.taskboard.exception.TooManyAttemptsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 아이디별 로그인 실패 횟수를 세어, 한도를 넘으면 잠시 잠금 (무차별 대입 방지).
 *
 * <p>IP 기준은 프록시(Caddy) 뒤라 X-Forwarded-For 신뢰 설정이 필요해 미사용.
 * 아이디 기준은 남의 계정을 일부러 잠글 수 있으므로 잠금 시간을 짧게 설정.
 * 메모리에만 저장하므로 재기동하면 초기화(단일 인스턴스 전제)
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    /** 이 시간 동안 실패가 없으면 카운트 초기화 */
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    /** 존재하지 않는 아이디로 무한히 시도해 맵을 부풀리는 것을 막는 상한 */
    private static final int MAX_TRACKED = 10_000;

    private final Map<String, Failures> failuresByUsername = new ConcurrentHashMap<>();

    public void checkNotLocked(String username) {
        Failures failures = failuresByUsername.get(key(username));
        if (failures != null && failures.isLocked(Instant.now())) {
            throw new TooManyAttemptsException(
                    "로그인 시도가 너무 많습니다. " + LOCK_DURATION.toMinutes() + "분 후에 다시 시도해 주세요.");
        }
    }

    public void recordFailure(String username) {
        Instant now = Instant.now();
        failuresByUsername.compute(key(username), (ignored, previous) -> {
            int count = (previous == null || previous.isStale(now)) ? 1 : previous.count() + 1;
            Instant lockedUntil = count >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null;
            return new Failures(count, now, lockedUntil);
        });
        purgeIfCrowded(now);
    }

    public void recordSuccess(String username) {
        failuresByUsername.remove(key(username));
    }

    /** Admin과 admin을 따로 세면 잠금 우회 가능 */
    private String key(String username) {
        return username == null ? "" : username.toLowerCase(Locale.ROOT);
    }

    private void purgeIfCrowded(Instant now) {
        if (failuresByUsername.size() <= MAX_TRACKED) return;
        failuresByUsername.values().removeIf(failures -> failures.isStale(now) && !failures.isLocked(now));
    }

    private record Failures(int count, Instant lastFailureAt, Instant lockedUntil) {

        boolean isLocked(Instant now) {
            return lockedUntil != null && lockedUntil.isAfter(now);
        }

        boolean isStale(Instant now) {
            return lastFailureAt.plus(FAILURE_WINDOW).isBefore(now);
        }
    }
}
