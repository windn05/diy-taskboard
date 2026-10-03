package com.taskboard.monitoring;

import com.sun.management.OperatingSystemMXBean;
import com.taskboard.monitoring.MetricsDtos.BackendStats;
import com.taskboard.monitoring.MetricsDtos.HostStats;
import com.taskboard.monitoring.MetricsDtos.SystemStatsResponse;
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

/** 모니터링 자원 지표 조회 (서버 전체와 백엔드 컨테이너·JVM을 구분) */
@Service
public class SystemStatsService {

    private static final long KB = 1024;
    private static final long MB = 1024 * 1024;
    private static final long GB = 1024 * 1024 * 1024;

    private static final Path PROC_STAT = Path.of("/proc/stat");
    private static final Path PROC_MEMINFO = Path.of("/proc/meminfo");
    private static final Path CGROUP_MEM_CURRENT = Path.of("/sys/fs/cgroup/memory.current");
    private static final Path CGROUP_MEM_MAX = Path.of("/sys/fs/cgroup/memory.max");

    /** 첫 조회처럼 비교할 직전 샘플이 없을 때, 두 번 읽는 사이의 간격 */
    private static final long FIRST_SAMPLE_GAP_MS = 200;

    /** {@code /proc/stat}의 CPU 사용률은 두 시점의 차이로만 구할 수 있어 직전 샘플을 보관 */
    private final AtomicReference<CpuTimes> previousCpu = new AtomicReference<>();

    /*************************************************************************
     * 목적 : 서버·백엔드 자원 지표 조회 (DB 항목은 비워 둠)
     * 이유 : DB 지표는 DataSource가 필요해 DbStatsService가 따로 채움
     * 파라미터
     * -
     * 반환
     * - 서버·백엔드 지표 (DB 항목은 비어 있음)
     *************************************************************************/
    public SystemStatsResponse snapshot() {
        var os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return new SystemStatsResponse(hostStats(os), backendStats(os), null);
    }

    /*************************************************************************
     * 목적 : 기동 시 CPU 사용률을 한 번 읽어 기준값 생성
     * 이유 : MXBean CPU 사용률은 직전 호출과의 차이라 첫 호출은 항상 0. 윈도우는 카운터가 약 1초 단위로 갱신돼
     *        요청 안에서 두 번 읽는 방식도 통하지 않음
     * 파라미터
     * -
     * 반환
     * -
     *************************************************************************/
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

        // 첫 조회는 비교 대상이 없어 잠깐 기다렸다가 한 번 더 조회
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
        // 한도가 없으면 "컨테이너 메모리"라는 개념이 성립하지 않으므로 둘 다 비움
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

    /*************************************************************************
     * 목적 : /proc/stat의 cpu 한 줄을 전체·유휴 시간으로 파싱
     * 이유 : iowait도 CPU가 일하지 않은 시간이라 유휴에 포함. guest 계열은 user에 이미 포함돼 중복
     *        합산하지 않음
     * 파라미터
     * - line : "cpu  user nice system idle ..." 형식의 한 줄
     * 반환
     * - 전체·유휴 시간 (형식이 다르면 null)
     *************************************************************************/
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

    /*************************************************************************
     * 목적 : /proc/stat의 cpu0, cpu1… 줄 수로 코어 수 계산 (합계 줄 cpu는 제외)
     * 이유 : -
     * 파라미터
     * - procStat : /proc/stat 전체 내용
     * 반환
     * - 코어 수
     *************************************************************************/
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

    /*************************************************************************
     * 목적 : cgroup v2 memory.max 값을 바이트로 해석 (한도 없음 "max"는 null)
     * 이유 : -
     * 파라미터
     * - raw : memory.max 파일 내용
     * 반환
     * - 한도 바이트 (한도 없음·형식 오류면 null)
     *************************************************************************/
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

    /*************************************************************************
     * 목적 : 지정 시간만큼 대기
     * 이유 : -
     * 파라미터
     * - millis : 대기 시간(ms)
     * 반환
     * - 정상 대기면 true, 인터럽트되면 false
     *************************************************************************/
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
