package com.jzargo.gatewayapi.handler;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class LeaderboardHandler {

    public static Mono<ServerResponse> getLeaderboardById(ServerRequest serverRequest) {
        String leaderboardId = serverRequest.pathVariable("leaderboard");

    }
}
