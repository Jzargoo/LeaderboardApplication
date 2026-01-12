package com.jzargo.websocketapi.lifecylce;

import com.jzargo.websocketapi.utils.PropertiesStorage;
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
public class SetAttributesInterceptor implements HandshakeInterceptor {

    private final PropertiesStorage propertiesStorage;

    public SetAttributesInterceptor(PropertiesStorage propertiesStorage) {
        this.propertiesStorage = propertiesStorage;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        URI uri = request.getURI();

        String query = uri.getQuery();

        Optional<String> lbId = Arrays.stream(query.split("&"))
                .filter(s -> s.startsWith(propertiesStorage
                        .getQuery().getLeaderboardId()))
                .map(s -> s.split("=")[1])
                .findFirst();

        Optional<String> userId = Optional.ofNullable(
                request.getHeaders().get(propertiesStorage
                                .getHeaders().getUserId())
                        .getFirst()
        );

        lbId.ifPresent(
                value -> attributes.put(propertiesStorage
                        .getAttribute().getLeaderboardId(),value)
        );

        userId.ifPresent(
                value -> attributes.put(propertiesStorage
                        .getAttribute().getUserId(),value)
        );

        return lbId.isPresent() && userId.isPresent();
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        log.debug("User successfully passed CheckLeaderboardIdInterceptor");
    }
}