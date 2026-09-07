package com.taskboard.service;

import com.taskboard.exception.TooManyAttemptsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로그인 실패를 아이디별로 세어 일정 횟수를 넘으면 잠시 막는다.
 *
 * <p>IP가 아니라 아이디를 기준으로 삼은 이유: 요청이 Caddy를 거쳐 오므로 실제 클라이언트 IP를
 * 알려면 X-Forwarded-For를 신뢰하도록 설정해야 하는데, 그 신뢰 자체가 새로운 가정이 된다.
 * 막으려는 공격이 "특정 계정의 비밀번호 맞히기"라 아이디 기준으로도 목적을 달성한다.
 *
 * <p>대신 남의 아이디를 일부러 잠글 수 있다는 트레이드오프가 있어, 잠금은 짧게만 건다.
 *
 * <p>상태를 메모리에만 두므로 재기동하면 초기화된다. 인스턴스가 하나인 현재 구성에서는 충분하다.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    /** 실패가 이 시간 동안 없으면 카운트를 처음부터 다시 센다. */
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    /** 존재하지 않는 아이디로 무한히 시도해 맵을 부풀리는 것을 막는 상한. */
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

    /** Admin과 admin을 따로 세면 잠금을 우회할 수 있다. */
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
