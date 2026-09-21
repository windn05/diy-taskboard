package com.taskboard.service;

import com.taskboard.dto.MetricsDtos.DbStats;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 이 서비스의 위험한 부분은 예외를 삼키는 경로다. 모니터링 지표 하나가 실패해도 화면 전체가
 * 죽지 않아야 하고, 그렇다고 실패를 매 폴링마다 반복해서도 안 된다.
 */
class DbStatsServiceTest {

    private DbStats snapshotWith(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return new DbStatsService(dataSource, jdbcTemplate).snapshot();
    }

    @Test
    void 풀이_HikariCP가_아니면_풀_지표는_비운다() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1024L * 1024 * 42);

        DbStats stats = snapshotWith(mock(DataSource.class), jdbc);

        assertThat(stats.poolActive()).isNull();
        assertThat(stats.poolMax()).isNull();
        assertThat(stats.poolWaiting()).isNull();
        // 풀을 못 읽는 것과 용량을 못 읽는 것은 별개다. 하나가 죽어도 다른 하나는 나와야 한다.
        assertThat(stats.sizeMb()).isEqualTo(42);
    }

    @Test
    void 용량_조회가_실패해도_스냅샷은_만들어진다() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        // Postgres가 아니면 pg_database_size 함수가 없어 예외가 난다 (테스트의 H2가 이 경우다).
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenThrow(new RuntimeException("no such function"));

        DbStats stats = snapshotWith(mock(DataSource.class), jdbc);

        assertThat(stats.sizeMb()).isNull();
    }

    @Test
    void 용량은_캐시되어_폴링마다_다시_조회하지_않는다() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1024L * 1024 * 7);
        DbStatsService service = new DbStatsService(mock(DataSource.class), jdbc);

        service.snapshot();
        service.snapshot();
        service.snapshot();

        assertThat(service.snapshot().sizeMb()).isEqualTo(7);
        verify(jdbc, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    void 실패도_캐시해서_실패_쿼리를_반복하지_않는다() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenThrow(new RuntimeException("no such function"));
        DbStatsService service = new DbStatsService(mock(DataSource.class), jdbc);

        service.snapshot();
        service.snapshot();

        verify(jdbc, times(1)).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    void HikariCP면_풀_지표를_읽는다() {
        HikariDataSource hikari = mock(HikariDataSource.class);
        when(hikari.getHikariPoolMXBean()).thenReturn(mock(com.zaxxer.hikari.HikariPoolMXBean.class));
        when(hikari.getMaximumPoolSize()).thenReturn(10);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

        DbStats stats = snapshotWith(hikari, jdbc);

        assertThat(stats.poolMax()).isEqualTo(10);
        assertThat(stats.poolActive()).isNotNull();
    }
}
