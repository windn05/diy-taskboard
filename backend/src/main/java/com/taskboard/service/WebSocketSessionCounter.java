package com.taskboard.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

/** 현재 WebSocket 연결 수. 모니터링 지표용 */
@Service
public class WebSocketSessionCounter {

    private final AtomicInteger active = new AtomicInteger();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        active.incrementAndGet();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        // CONNECT 전에 끊긴 세션도 DISCONNECT는 오므로 음수가 되지 않게 방지
        active.updateAndGet(count -> Math.max(0, count - 1));
    }

    public int active() {
        return active.get();
    }
}
