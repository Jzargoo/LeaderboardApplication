package com.jzargo.websocketapi.config.WebSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserIdAsPrincipalHandshakeHandler userIdAsPrincipalHandshakeHandler;

    public WebSocketConfig(UserIdAsPrincipalHandshakeHandler userIdAsPrincipalHandshakeHandler) {
        this.userIdAsPrincipalHandshakeHandler = userIdAsPrincipalHandshakeHandler;
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/leaderboard")
                .setHandshakeHandler(userIdAsPrincipalHandshakeHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }


}
