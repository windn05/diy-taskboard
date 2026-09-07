package com.taskboard.config;

import com.taskboard.service.RequestMetricsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RequestMetricsInterceptor implements HandlerInterceptor {

    private static final String START_TIME = RequestMetricsInterceptor.class.getName() + ".start";

    private final RequestMetricsService metricsService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(START_TIME, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        Object start = request.getAttribute(START_TIME);
        if (start == null) return;
        long durationMs = (System.nanoTime() - (Long) start) / 1_000_000;
        metricsService.record(durationMs, response.getStatus());
    }
}
