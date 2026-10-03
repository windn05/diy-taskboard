package com.taskboard.monitoring;

import com.taskboard.monitoring.MetricsDtos.DbStats;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;

/** 모니터링 DB 지표 (커넥션 풀 상태, DB 용량) */
@Service
public class DbStatsService {

    private static final long MB = 1024 * 1024;

    /** DB 용량 재조회 간격. 화면 폴링 주기(5초)보다 충분히 길게 */
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

    /*************************************************************************
     * 목적 : 커넥션 풀(HikariCP) 상태 조회용 MXBean 얻기 (HikariCP가 아니면 null)
     * 이유 : -
     * 파라미터
     * -
     * 반환
     * - 풀 MXBean (HikariCP가 아니면 null)
     *************************************************************************/
    private HikariPoolMXBean hikariPool() {
        return dataSource instanceof HikariDataSource hikari ? hikari.getHikariPoolMXBean() : null;
    }

    private Integer maxPoolSize() {
        return dataSource instanceof HikariDataSource hikari ? hikari.getMaximumPoolSize() : null;
    }

    /*************************************************************************
     * 목적 : DB 용량(MB) 조회 (Postgres 전용, 실패하면 null)
     * 이유 : 다른 DB(테스트의 H2)에서 실패해도 모니터링 화면 전체가 깨지지 않게 함
     * 파라미터
     * -
     * 반환
     * - 용량 MB (조회 불가 시 null)
     *************************************************************************/
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
        // 실패도 캐시. Postgres가 아니면 5초마다 실패 쿼리를 날릴 이유가 없음
        cachedSize.set(new CachedSize(bytes, now));
        return bytes == null ? null : bytes / MB;
    }
}
