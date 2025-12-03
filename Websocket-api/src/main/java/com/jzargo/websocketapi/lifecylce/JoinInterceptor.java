package com.jzargo.websocketapi.lifecylce;

import com.jzargo.websocketapi.service.WebSocketService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JoinInterceptor implements HandshakeInterceptor {

    private final WebSocketService webSocketService;
    private final FeignRequestInterceptor feignRequestInterceptor;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        try {

            URI uri = request.getURI();

            String query = uri.getQuery();

            Optional<String> lbId = Arrays.stream(query.split("&"))
                    .filter(s -> s.startsWith(CheckLeaderboardIdInterceptor.LEADERBOARD_QUERY + "="))
                    .map(s -> s.split("=")[1])
                    .findFirst();

            if(lbId.isEmpty()) {
                return false;
            }

            List<String> authHeaders = request.getHeaders().get("Authorization");

            boolean isSuccess = false;

            if (authHeaders != null && !authHeaders.isEmpty()) {

                String authHeader = authHeaders.getFirst();

                if (authHeader.startsWith("Bearer ")) {
                    String jwt = authHeader.substring(7);

                    feignRequestInterceptor.setJwt(jwt);
                    try {
                    webSocketService.initLeaderboardScore(lbId.get());
                    isSuccess = true;
                    }  catch (Exception e) {
                        log.error("Caught error while processing handshake", e);
                    }
                }
            }

            return isSuccess;

        } catch (FeignException.FeignClientException e) {

            log.error("request ended with client exception");

            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;

        } catch (FeignException.FeignServerException e) {

            log.error("request ended with server exception");

            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;

        }
}

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        log.debug("User successfully passed JoinInterceptor");
    }
}
