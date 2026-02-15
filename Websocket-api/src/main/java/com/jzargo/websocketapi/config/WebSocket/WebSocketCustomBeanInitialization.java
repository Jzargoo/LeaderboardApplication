package com.jzargo.websocketapi.config.WebSocket;

import com.jzargo.websocketapi.config.properties.ApplicationPropertiesStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketCustomBeanInitialization {
    @Bean
    UserIdAsPrincipalHandshakeHandler userIdAsPrincipalHandshakeHandler(ApplicationPropertiesStorage ps){
        return new UserIdAsPrincipalHandshakeHandler(ps);
    }
}
