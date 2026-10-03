package com.taskboard.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 아이디별 로그인 실패를 세어 한도를 넘으면 잠시 잠금 (무차별 대입 방지) */
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

    /*************************************************************************
     * 목적 : 실패 횟수 집계용 키 생성 (아이디를 소문자로 통일)
     * 이유 : Admin과 admin을 따로 세면 잠금을 우회할 수 있음
     * 파라미터
     * - username : 입력된 아이디
     * 반환
     * - 소문자 아이디
     *************************************************************************/
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
