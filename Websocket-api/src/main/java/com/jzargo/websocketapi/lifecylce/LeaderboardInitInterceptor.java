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

import java.util.Arrays;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class LeaderboardInitInterceptor implements HandshakeInterceptor {

    private final WebSocketService webSocketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {

            String lbId = Arrays.toString(
                    request.getBody()
                            .readAllBytes()
            );
            webSocketService.initLeaderboardScore(lbId);
            return true;
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
    }
}
