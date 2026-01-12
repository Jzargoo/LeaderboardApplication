package com.jzargo.websocketapi.config.WebSocket;

import com.jzargo.websocketapi.utils.PropertiesStorage;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class UserIdAsPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    private final PropertiesStorage propertiesStorage;

    public UserIdAsPrincipalHandshakeHandler (PropertiesStorage propertiesStorage) {
        this.propertiesStorage = propertiesStorage;
    }

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String id = (String) attributes.get(propertiesStorage.getAttribute().getUserId());
        return () -> id;
    }
}