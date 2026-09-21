package com.taskboard.service;

import com.sun.management.OperatingSystemMXBean;
import com.taskboard.dto.MetricsDtos.BackendStats;
import com.taskboard.dto.MetricsDtos.HostStats;
import com.taskboard.dto.MetricsDtos.SystemStatsResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 모니터링 화면의 자원 지표를 "서버(VM) 전체"와 "백엔드(컨테이너·JVM)"로 나눠 조회한다.
 *
 * <p>둘을 따로 읽는 이유: 백엔드는 컨테이너 안에서 돌고 JVM은 컨테이너를 인식하므로,
 * MXBean이 주는 메모리·CPU는 <b>컨테이너 기준</b>이다. 운영에서 "서버 메모리"가 VM의 954MB가 아니라
 * 컨테이너 한도 450MB로 보였다. 그래서
 * <ul>
 *   <li>서버 전체 — 컨테이너 안에서도 호스트 값을 그대로 보여주는 {@code /proc/meminfo}, {@code /proc/stat}</li>
 *   <li>컨테이너 사용량 — cgroup v2의 {@code memory.current}, {@code memory.max}</li>
 *   <li>JVM — MXBean</li>
 * </ul>
 * 에서 각각 읽는다.
 *
 * <p>{@code /proc}가 없는 환경(로컬 윈도우 개발)에서는 MXBean 값으로 대체한다 — 컨테이너 밖이면 그게 곧 호스트 값이다.
 * JDK 내장 기능만 쓰고 별도 라이브러리는 추가하지 않는다.
 */
@Service
public class SystemStatsService {

    private static final long KB = 1024;
    private static final long MB = 1024 * 1024;
    private static final long GB = 1024 * 1024 * 1024;

    private static final Path PROC_STAT = Path.of("/proc/stat");
    private static final Path PROC_MEMINFO = Path.of("/proc/meminfo");
    private static final Path CGROUP_MEM_CURRENT = Path.of("/sys/fs/cgroup/memory.current");
    private static final Path CGROUP_MEM_MAX = Path.of("/sys/fs/cgroup/memory.max");

    /** 첫 조회처럼 비교할 직전 샘플이 없을 때, 두 번 읽는 사이에 둘 간격. */
    private static final long FIRST_SAMPLE_GAP_MS = 200;

    /** {@code /proc/stat}의 CPU 사용률은 두 시점의 차이로만 구할 수 있어 직전 샘플을 들고 있는다. */
    private final AtomicReference<CpuTimes> previousCpu = new AtomicReference<>();

    /** DB 지표는 DataSource가 필요해 이 클래스가 다루지 않는다 — 컨트롤러가 DbStatsService의 값을 채워 넣는다. */
    public SystemStatsResponse snapshot() {
        var os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return new SystemStatsResponse(hostStats(os), backendStats(os), null);
    }

    /**
     * MXBean의 CPU 사용률은 직전 호출과의 차이로 계산해서, 한 번도 읽은 적이 없으면 0이 나온다.
     * 기동할 때 미리 한 번 읽어 기준을 만들어 둔다.
     *
     * <p>요청 안에서 두 번 읽는 방식은 쓰지 않았다. 윈도우의 시스템 CPU 카운터는 약 1초 단위로 갱신돼
     * 짧은 간격으로는 계속 0이 나왔고("서버 CPU 0%, 프로세스 CPU 8%"처럼 전체가 부분보다 작게 보였다),
     * 그렇다고 요청마다 1초를 기다리게 할 수는 없다.
     */
    @PostConstruct
    void primeMxbeanCpu() {
        var os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        os.getCpuLoad();
        os.getProcessCpuLoad();
    }

    // --- 서버 전체 ---------------------------------------------------------

    private HostStats hostStats(OperatingSystemMXBean os) {
        List<String> procStat = readLines(PROC_STAT);
        MemInfo memInfo = parseMeminfo(readLines(PROC_MEMINFO));

        double cpuPercent = procStat == null ? toPercent(os.getCpuLoad()) : hostCpuPercent(procStat);
        int cpuCores = procStat == null ? os.getAvailableProcessors() : countCores(procStat);

        long memTotal = memInfo != null ? memInfo.totalBytes() : os.getTotalMemorySize();
        long memUsed = memInfo != null
                ? memInfo.totalBytes() - memInfo.availableBytes()
                : os.getTotalMemorySize() - os.getFreeMemorySize();

        File disk = new File(".");
        return new HostStats(
                cpuPercent,
                cpuCores,
                memUsed / MB,
                memTotal / MB,
                (disk.getTotalSpace() - disk.getUsableSpace()) / GB,
                disk.getTotalSpace() / GB);
    }

    private double hostCpuPercent(List<String> procStat) {
        CpuTimes current = parseCpuLine(procStat.get(0));
        CpuTimes previous = previousCpu.getAndSet(current);
        if (previous != null) return cpuPercent(previous, current);

        // 첫 조회는 비교 대상이 없어 잠깐 기다렸다가 한 번 더 읽는다.
        if (!sleepQuietly(FIRST_SAMPLE_GAP_MS)) return 0;
        List<String> again = readLines(PROC_STAT);
        if (again == null) return 0;
        CpuTimes next = parseCpuLine(again.get(0));
        previousCpu.set(next);
        return cpuPercent(current, next);
    }

    // --- 백엔드 ------------------------------------------------------------

    private BackendStats backendStats(OperatingSystemMXBean os) {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        Long containerUsed = readLong(CGROUP_MEM_CURRENT);
        Long containerLimit = parseCgroupLimit(readFirstLine(CGROUP_MEM_MAX));
        // 한도가 없으면 "컨테이너 메모리"라는 개념 자체가 성립하지 않으므로 둘 다 비운다.
        boolean limited = containerUsed != null && containerLimit != null;

        return new BackendStats(
                toPercent(os.getProcessCpuLoad()),
                limited ? containerUsed / MB : null,
                limited ? containerLimit / MB : null,
                heap.getUsed() / MB,
                heap.getMax() / MB,
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
    }

    // --- 파싱 (파일 없이 테스트할 수 있게 분리) -------------------------------

    record CpuTimes(long idle, long total) {
    }

    record MemInfo(long totalBytes, long availableBytes) {
    }

    /**
     * {@code cpu  user nice system idle iowait irq softirq steal ...} 한 줄을 읽는다.
     * iowait도 CPU가 일을 하지 않은 시간이라 idle에 넣는다. guest 계열은 user에 이미 포함돼 있어 합산하지 않는다.
     */
    static CpuTimes parseCpuLine(String line) {
        String[] f = line.trim().split("\\s+");
        long user = Long.parseLong(f[1]);
        long nice = Long.parseLong(f[2]);
        long system = Long.parseLong(f[3]);
        long idle = Long.parseLong(f[4]);
        long iowait = Long.parseLong(f[5]);
        long irq = Long.parseLong(f[6]);
        long softirq = Long.parseLong(f[7]);
        long steal = f.length > 8 ? Long.parseLong(f[8]) : 0;
        return new CpuTimes(idle + iowait, user + nice + system + idle + iowait + irq + softirq + steal);
    }

    static double cpuPercent(CpuTimes previous, CpuTimes current) {
        long total = current.total() - previous.total();
        if (total <= 0) return 0;
        long busy = total - (current.idle() - previous.idle());
        double percent = Math.round(busy * 1000.0 / total) / 10.0;
        return Math.max(0, Math.min(100, percent));
    }

    /** {@code cpu0}, {@code cpu1}… 줄 수. 맨 앞의 합계 줄 {@code cpu}는 세지 않는다. */
    static int countCores(List<String> procStat) {
        return (int) procStat.stream().filter(line -> line.matches("^cpu\\d+\\s.*")).count();
    }

    static MemInfo parseMeminfo(List<String> lines) {
        if (lines == null) return null;
        Long total = null;
        Long available = null;
        for (String line : lines) {
            if (line.startsWith("MemTotal:")) total = kbField(line);
            else if (line.startsWith("MemAvailable:")) available = kbField(line);
        }
        return total != null && available != null ? new MemInfo(total, available) : null;
    }

    /** cgroup v2의 {@code memory.max}. 한도가 없으면 {@code max}라고 적혀 있다. */
    static Long parseCgroupLimit(String raw) {
        if (raw == null || raw.isBlank() || raw.trim().equals("max")) return null;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long kbField(String line) {
        return Long.parseLong(line.replaceAll("[^0-9]", "")) * KB;
    }

    /** 인터럽트되면 false. */
    private static boolean sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static double toPercent(double load) {
        return load < 0 ? 0 : Math.round(load * 1000) / 10.0;
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.isReadable(path) ? Files.readAllLines(path) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static String readFirstLine(Path path) {
        List<String> lines = readLines(path);
        return lines == null || lines.isEmpty() ? null : lines.get(0);
    }

    private static Long readLong(Path path) {
        return parseCgroupLimit(readFirstLine(path));
    }
}
