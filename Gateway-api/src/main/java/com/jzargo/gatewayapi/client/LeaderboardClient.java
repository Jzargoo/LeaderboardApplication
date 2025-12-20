package com.jzargo.gatewayapi.client;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.gatewayapi.dto.UserScoreResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class LeaderboardClient {

    private final WebClient webClient;

    @Value("${app.leaderboard-service-url:http://localhost:8080}")
    private String leaderboardServiceUrl;

    public LeaderboardClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<UserScoreResponse> getUserScore(String id){
        return webClient.get()
                .uri(leaderboardServiceUrl + "/api/v1/leaderboard/view/user/{id}", id)
                .retrieve()
                .bodyToMono(UserScoreResponse.class);
    }


    public Mono<LeaderboardResponse> getLeaderboard(String id){
        return webClient.get()
                .uri(leaderboardServiceUrl + "/api/v1/leaderboard/view/{id}", id)
                .retrieve()
                .bodyToMono(LeaderboardResponse.class);
    }
}
