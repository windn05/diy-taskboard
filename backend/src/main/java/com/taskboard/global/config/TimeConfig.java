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

/** 시각 처리 설정 (DB·서버는 UTC, 화면 표시는 브라우저가 현지 시각으로 변환) */
@Configuration
public class TimeConfig {

    /** 업무 날짜 기준 시간대. 마감일 등 LocalDate는 이 시간대의 달력 날짜 */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /*************************************************************************
     * 목적 : 날짜·시각을 끝에 Z를 붙인 UTC 표기로 내보내는 모듈 등록 (REST·STOMP 공통)
     * 이유 : Z가 없으면 브라우저가 로컬 시각으로 해석해 9시간 어긋남
     * 파라미터
     * -
     * 반환
     * - Jackson 모듈
     *************************************************************************/
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
