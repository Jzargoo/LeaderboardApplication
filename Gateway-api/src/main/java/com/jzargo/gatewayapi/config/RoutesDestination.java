package com.jzargo.gatewayapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoutesDestination {
    @Value("${app.leaderboard-service-url:http://localhost:8080}")
    private String leaderboardServiceUrl;

    public String getLeaderboardServiceUrl() {
        return leaderboardServiceUrl;
    }

    public void setLeaderboardServiceUrl(String leaderboardServiceUrl) {
        this.leaderboardServiceUrl = leaderboardServiceUrl;
    }
}
