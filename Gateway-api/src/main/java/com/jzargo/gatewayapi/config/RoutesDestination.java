package com.jzargo.gatewayapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class RoutesDestination {
    @Value("${app.leaderboard-service-url:http://localhost:8080}")
    private String leaderboardServiceUrl;

}
