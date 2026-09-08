package com.taskboard.service;

import com.sun.management.OperatingSystemMXBean;
import com.taskboard.dto.MetricsDtos.SystemStatsResponse;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

/** 호스트(VM/컨테이너) 자원 현황을 그때그때 조회한다. JDK 내장 MXBean만 쓰고 별도 라이브러리는 추가하지 않는다. */
@Service
public class SystemStatsService {

    private static final long MB = 1024 * 1024;
    private static final long GB = 1024 * 1024 * 1024;

    public SystemStatsResponse snapshot() {
        var os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        File disk = new File(".");

        double cpuLoad = os.getCpuLoad();
        long systemMemTotal = os.getTotalMemorySize();
        long systemMemFree = os.getFreeMemorySize();

        return new SystemStatsResponse(
                cpuLoad < 0 ? 0 : Math.round(cpuLoad * 1000) / 10.0,
                os.getAvailableProcessors(),
                heap.getUsed() / MB,
                heap.getMax() / MB,
                (systemMemTotal - systemMemFree) / MB,
                systemMemTotal / MB,
                (disk.getTotalSpace() - disk.getUsableSpace()) / GB,
                disk.getTotalSpace() / GB,
                ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
    }
}
