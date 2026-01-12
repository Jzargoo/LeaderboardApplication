package com.jzargo.websocketapi.config.WebSocket;

import com.jzargo.websocketapi.utils.PropertiesStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserIdAsPrincipalHandshakeHandler userIdAsPrincipalHandshakeHandler;

    public WebSocketConfig(UserIdAsPrincipalHandshakeHandler userIdAsPrincipalHandshakeHandler) {
        this.userIdAsPrincipalHandshakeHandler = userIdAsPrincipalHandshakeHandler;
    }

    @Bean
    UserIdAsPrincipalHandshakeHandler userIdAsPrincipalHandshakeHandler(PropertiesStorage ps){
        return new UserIdAsPrincipalHandshakeHandler(ps);
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
