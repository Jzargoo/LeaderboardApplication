package com.jzargo.websocketapi.lifecylce;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;

@Component
public class LeaderboardJoinEventListener {
    @EventListener
    public void handleConnectRequest(SessionConnectEvent event) {

    }
}
