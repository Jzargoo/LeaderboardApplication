package com.jzargo.websocketapi.lifecylce;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class SubscribeInterceptor implements ChannelInterceptor {
    public static final String LOCAL_LEADERBOARD_UPDATE_ENDPOINT = "user/queue/leaderboard-update/";
    public static final String GLOBAL_LEADERBOARD_UPDATE_ENDPOINT = "/topic/leaderboard-update/";

    @Override
    public Message<?> preSend(@SuppressWarnings("NullableProblems") Message<?> message,
                              @SuppressWarnings("NullableProblems") MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);


        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            JwtAuthenticationToken auth = (JwtAuthenticationToken) accessor.getUser();
            Long userId = Long.valueOf(Objects.requireNonNull(auth)
                    .getToken().getClaimAsString("user_id"));

            Objects.requireNonNull(
                    accessor.getSessionAttributes()
                    ).put("userId", userId);

            accessor.setUser(
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of()));
        }
        return message;
    }
}
