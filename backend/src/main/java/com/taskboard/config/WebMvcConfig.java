package com.taskboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** MVC 인터셉터 등록 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestMetricsInterceptor requestMetricsInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(requestMetricsInterceptor)
                .addPathPatterns("/**")
                // 대시보드가 주기적으로 폴링하는 엔드포인트라, 집계에 넣으면 자기 트래픽이 통계를 지배함
                .excludePathPatterns("/admin/metrics");
    }
}
