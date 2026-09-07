package com.taskboard.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WebSocketSessionCounter {

    private final AtomicInteger active = new AtomicInteger();

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        active.incrementAndGet();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        active.updateAndGet(count -> Math.max(0, count - 1));
    }

    public int active() {
        return active.get();
    }
}
