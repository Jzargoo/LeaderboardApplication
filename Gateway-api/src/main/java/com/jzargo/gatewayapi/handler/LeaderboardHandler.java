package com.jzargo.gatewayapi.handler;

import com.jzargo.gatewayapi.client.LeaderboardClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class LeaderboardHandler {
    private final LeaderboardClient leaderboardClient;

    public LeaderboardHandler(LeaderboardClient leaderboardClient) {
        this.leaderboardClient = leaderboardClient;
    }

    public Mono<ServerResponse> getLeaderboardById(ServerRequest serverRequest) {
        String leaderboardId = serverRequest.pathVariable("leaderboard");
        return Mono.from(
                ServerResponse.ok().bodyValue(
                        leaderboardClient
                                .getLeaderboard(leaderboardId))
                );
    }
}
