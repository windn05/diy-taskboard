package com.taskboard.service;

import com.taskboard.service.SystemStatsService.CpuTimes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemStatsServiceTest {

    @Test
    void iowait도_유휴시간으로_센다() {
        // user nice system idle iowait irq softirq steal guest guest_nice
        CpuTimes times = SystemStatsService.parseCpuLine("cpu  100 0 50 800 50 0 0 0 0 0");

        assertThat(times.idle()).isEqualTo(850);
        assertThat(times.total()).isEqualTo(1000);
    }

    @Test
    void 두_시점의_차이로_사용률을_구한다() {
        CpuTimes before = new CpuTimes(800, 1000);
        CpuTimes after = new CpuTimes(850, 1100); // 100 중 50이 유휴

        assertThat(SystemStatsService.cpuPercent(before, after)).isEqualTo(50.0);
    }

    @Test
    void 시간이_흐르지_않았으면_0이다() {
        CpuTimes same = new CpuTimes(800, 1000);

        assertThat(SystemStatsService.cpuPercent(same, same)).isZero();
    }

    @Test
    void 합계_줄은_코어로_세지_않는다() {
        List<String> procStat = List.of(
                "cpu  103799 2324 80901 28217501 21802 0 519 180814 0 0",
                "cpu0 51899 1162 40450 14108750 10901 0 259 90407 0 0",
                "cpu1 51900 1162 40451 14108751 10901 0 260 90407 0 0",
                "intr 12345");

        assertThat(SystemStatsService.countCores(procStat)).isEqualTo(2);
    }

    /** 운영 서버 컨테이너 안에서 실제로 읽힌 값 — VM 전체(954MB)여야 하고 컨테이너 한도(450MB)가 아니어야 한다. */
    @Test
    void meminfo는_kB를_바이트로_읽는다() {
        var memInfo = SystemStatsService.parseMeminfo(List.of(
                "MemTotal:         976904 kB",
                "MemFree:          120000 kB",
                "MemAvailable:     376912 kB"));

        assertThat(memInfo).isNotNull();
        assertThat(memInfo.totalBytes() / (1024 * 1024)).isEqualTo(954);
        assertThat(memInfo.availableBytes()).isEqualTo(376912L * 1024);
    }

    @Test
    void meminfo에_필요한_항목이_없으면_null이다() {
        assertThat(SystemStatsService.parseMeminfo(List.of("MemTotal: 976904 kB"))).isNull();
        assertThat(SystemStatsService.parseMeminfo(null)).isNull();
    }

    @Test
    void cgroup_한도가_max면_제한이_없는_것이다() {
        assertThat(SystemStatsService.parseCgroupLimit("max")).isNull();
        assertThat(SystemStatsService.parseCgroupLimit("471859200")).isEqualTo(471859200L);
        assertThat(SystemStatsService.parseCgroupLimit(null)).isNull();
    }

    /** 실행 환경에 /proc가 없든(윈도우) 있든(CI 리눅스) 조회 자체는 실패하지 않아야 한다. */
    @Test
    void 어느_환경에서든_스냅샷을_만든다() {
        var stats = new SystemStatsService().snapshot();

        assertThat(stats.host().memTotalMb()).isPositive();
        assertThat(stats.host().cpuCores()).isPositive();
        assertThat(stats.backend().heapMaxMb()).isPositive();
        assertThat(stats.host().cpuPercent()).isBetween(0.0, 100.0);
    }
}
