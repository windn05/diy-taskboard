package com.taskboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** 시각은 UTC임을 알 수 있게 Z를 붙여 전송 — 없으면 브라우저가 로컬 시각으로 읽어 9시간 어긋남 */
@SpringBootTest
@ActiveProfiles("test")
class UtcTimeSerializationTest {

    @Autowired ObjectMapper objectMapper;
    @Autowired SimpMessagingTemplate messagingTemplate;

    record Payload(LocalDateTime at) {
    }

    private static final Payload SAMPLE = new Payload(LocalDateTime.of(2026, 9, 21, 5, 30, 12, 123_456_789));
    private static final String EXPECTED = "\"at\":\"2026-09-21T05:30:12.123Z\"";

    @Test
    void REST_응답은_UTC_표기로_직렬화된다() throws Exception {
        assertThat(objectMapper.writeValueAsString(SAMPLE)).contains(EXPECTED);
    }

    @Test
    void 실시간_메시지도_같은_형식으로_직렬화된다() {
        Message<?> message = messagingTemplate.getMessageConverter().toMessage(SAMPLE, null);

        assertThat(message).isNotNull();
        assertThat(new String((byte[]) message.getPayload(), StandardCharsets.UTF_8)).contains(EXPECTED);
    }
}
