package com.jzargo.gatewayapi.config;

import com.jzargo.gatewayapi.client.LeaderboardClient;
import com.jzargo.gatewayapi.handler.LeaderboardHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
@EnableWebFlux
public class RoutingConfiguration {
    @Bean
    public LeaderboardHandler leaderboardHandler(LeaderboardClient leaderboardClient){
        return new LeaderboardHandler(leaderboardClient);
    }
    @Bean
    public RouterFunction<ServerResponse> routeGettingLeaderboard(LeaderboardHandler leaderboardHandler) {
        return RouterFunctions
             .route(GET("/leaderboard/{leaderboardId}"),
                     leaderboardHandler::getLeaderboardById);
    }

    @Bean
    public WebClient webClient(){
        return WebClient.builder().build();
    }
}
