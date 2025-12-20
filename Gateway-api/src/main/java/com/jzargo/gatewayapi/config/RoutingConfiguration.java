package com.jzargo.gatewayapi.config;

import com.jzargo.gatewayapi.handler.LeaderboardHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.*;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
@EnableWebFlux
public class RoutingConfiguration {
    @Bean
    public LeaderboardHandler leaderboardHandler(){
        return new LeaderboardHandler();
    }
    @Bean
    public RouterFunction<ServerResponse> routeGettingLeaderboard() {
        return RouterFunctions
             .route(GET("/leaderboard/{leaderboardId}"),
                     LeaderboardHandler::getLeaderboardById);
    }

    @Bean
    public WebClient webClient(){
        return WebClient.builder().build();
    }
}
