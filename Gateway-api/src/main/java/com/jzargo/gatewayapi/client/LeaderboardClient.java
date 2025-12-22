package com.jzargo.gatewayapi.client;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.gatewayapi.config.RoutesDestination;
import com.jzargo.gatewayapi.dto.UserScoreResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class LeaderboardClient {

    private final WebClient webClient;
    private final RoutesDestination routesDestination;

    public LeaderboardClient(WebClient webClient, RoutesDestination routesDestination) {
        this.webClient = webClient;
        this.routesDestination = routesDestination;
    }

    public Mono<UserScoreResponse> getUserScore(String id){
        return webClient.get()
                .uri( routesDestination.getLeaderboardServiceUrl()+ "/api/v1/leaderboard/view/user/{id}", id)
                .retrieve()
                .bodyToMono(UserScoreResponse.class);
    }


    public Mono<LeaderboardResponse> getLeaderboard(String id){
        return webClient.get()
                .uri(routesDestination.getLeaderboardServiceUrl()+ "/api/v1/leaderboard/view/{id}", id)
                .retrieve()
                .bodyToMono(LeaderboardResponse.class);
    }
}
