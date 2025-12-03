package com.jzargo.websocketapi.lifecylce;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class CheckLeaderboardIdInterceptor implements HandshakeInterceptor {
    public static String LEADERBOARD_ATTRIBUTE = "leaderboard_id";
    public static String LEADERBOARD_QUERY = "leaderboardId";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        URI uri = request.getURI();

        String query = uri.getQuery();

        Optional<String> lbId = Arrays.stream(query.split("&"))
                .filter(s -> s.startsWith(LEADERBOARD_QUERY))
                .map(s -> s.split("=")[1])
                .findFirst();

        lbId.ifPresent(
                value -> attributes.put(LEADERBOARD_ATTRIBUTE,value)
        );

        return lbId.isPresent();
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        log.debug("User successfully passed CheckLeaderboardIdInterceptor");
    }
}