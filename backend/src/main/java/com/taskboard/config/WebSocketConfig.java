package com.taskboard.config;

import com.taskboard.security.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

/**
 * STOMP over WebSocket 설정. 단일 인스턴스라 외부 브로커 없이 내장 SimpleBroker 사용.
 * 서버를 여러 대로 늘리면 인스턴스 간 메시지가 공유되지 않으므로 외부 브로커로 교체 필요
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 세션 쿠키는 SameSite=Lax라 다른 오리진에서 보낸 핸드셰이크엔 애초에 실리지 않으므로 Origin 제한 불필요.
        // HttpSessionHandshakeInterceptor로 HTTP 세션(SecurityContext 포함)을 WebSocket 세션에 그대로 복사한다.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // CONNECT 인증과 SUBSCRIBE 권한 검사
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
