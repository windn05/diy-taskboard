package com.taskboard.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 시각 처리 규칙. DB·서버는 UTC, 화면 표시는 브라우저(KST)가 변환.
 * <ul>
 *   <li>엔티티의 LocalDateTime은 UTC 기준 값 (JVM 기본 시간대를 UTC로 고정 — TaskboardApplication)</li>
 *   <li>응답에는 끝에 Z를 붙여 UTC임을 명시 — 없으면 브라우저가 로컬 시각으로 해석</li>
 *   <li>"오늘"처럼 날짜 경계가 필요한 계산은 {@link #BUSINESS_ZONE} 기준</li>
 * </ul>
 */
@Configuration
public class TimeConfig {

    /** 업무 날짜 기준 시간대. 마감일 등 LocalDate는 이 시간대의 달력 날짜 */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /** Spring Boot가 Module 빈을 ObjectMapper에 자동 등록 — REST 응답과 STOMP 메시지 모두 적용 */
    @Bean
    public Module utcLocalDateTimeModule() {
        return utcModule();
    }

    public static SimpleModule utcModule() {
        SimpleModule module = new SimpleModule("utc-local-date-time");
        module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(UTC_FORMAT.format(value));
            }
        });
        return module;
    }
}
