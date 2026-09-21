package com.taskboard.service;

import com.taskboard.dto.MetricsDtos.DbStats;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 모니터링 화면의 DB 지표.
 *
 * <p>{@link SystemStatsService}와 나눠 둔 이유는 출처가 다르기 때문이다. 그쪽은 {@code /proc}·cgroup·MXBean에서
 * 읽어 의존성이 없지만, 여기는 커넥션 풀과 DB 자체를 봐야 해서 {@link DataSource}가 필요하다.
 *
 * <p>풀 상태는 MXBean 조회라 사실상 공짜지만 DB 용량은 데이터 디렉터리를 훑는 쿼리다. 화면이 5초마다
 * 폴링하는데 용량은 초 단위로 변하지 않으므로, 용량만 {@link #SIZE_TTL_MS} 동안 캐시한다.
 */
@Service
public class DbStatsService {

    private static final long MB = 1024 * 1024;

    /** DB 용량을 다시 재기까지 기다리는 시간. 화면 폴링 주기(5초)보다 충분히 길게 둔다. */
    private static final long SIZE_TTL_MS = 60_000;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final AtomicReference<CachedSize> cachedSize = new AtomicReference<>();

    private record CachedSize(Long bytes, long measuredAtMillis) {
    }

    public DbStatsService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    public DbStats snapshot() {
        HikariPoolMXBean pool = hikariPool();
        return new DbStats(
                pool == null ? null : pool.getActiveConnections(),
                pool == null ? null : pool.getIdleConnections(),
                pool == null ? null : maxPoolSize(),
                pool == null ? null : pool.getThreadsAwaitingConnection(),
                databaseSizeMb());
    }

    /** 풀 구현이 HikariCP가 아니면 이 지표들은 성립하지 않는다. */
    private HikariPoolMXBean hikariPool() {
        return dataSource instanceof HikariDataSource hikari ? hikari.getHikariPoolMXBean() : null;
    }

    private Integer maxPoolSize() {
        return dataSource instanceof HikariDataSource hikari ? hikari.getMaximumPoolSize() : null;
    }

    /**
     * Postgres 전용 조회다. 다른 DB(테스트의 H2)에서는 함수가 없어 예외가 나므로 null로 넘긴다 —
     * 모니터링 지표 하나 때문에 화면 전체가 실패하면 안 된다.
     */
    private Long databaseSizeMb() {
        CachedSize cached = cachedSize.get();
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.measuredAtMillis() < SIZE_TTL_MS) {
            return cached.bytes() == null ? null : cached.bytes() / MB;
        }

        Long bytes;
        try {
            bytes = jdbcTemplate.queryForObject("select pg_database_size(current_database())", Long.class);
        } catch (Exception e) {
            bytes = null;
        }
        // 실패도 캐시한다. Postgres가 아니면 5초마다 계속 실패 쿼리를 날릴 이유가 없다.
        cachedSize.set(new CachedSize(bytes, now));
        return bytes == null ? null : bytes / MB;
    }
}
