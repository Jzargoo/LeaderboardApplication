package com.jzargo.websocketapi.config.WebSocket;

import com.jzargo.websocketapi.config.properties.ApplicationPropertiesStorage;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class UserIdAsPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    private final ApplicationPropertiesStorage applicationPropertiesStorage;

    public UserIdAsPrincipalHandshakeHandler (ApplicationPropertiesStorage applicationPropertiesStorage) {
        this.applicationPropertiesStorage = applicationPropertiesStorage;
    }

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String id = (String) attributes.get(applicationPropertiesStorage.getAttribute().getUserId());
        return () -> id;
    }
}