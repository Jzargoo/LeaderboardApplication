package com.jzargo.websocketapi.lifecylce;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.websocketapi.exception.InvalidDestinationException;
import com.jzargo.websocketapi.service.LeaderboardWebClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NullArgumentException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscribeSTOMPInterceptor implements ChannelInterceptor {


    private final LeaderboardWebClient leaderboardWebClient;
    private final PropertiesStorage propertiesStorage;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void postSend(@NonNull Message<?> message, @NonNull MessageChannel channel, boolean isSent) {

        if (!isSent) return;

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        //if accessor does not have destination, it will not a valid message,
        //so I should stop it by throwing exception
        if (accessor.getDestination() ==null) {
            throw new NullArgumentException("Destination in accessor for subscribe stomp interceptor is null");
        }

        if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
            if(accessor.getDestination()
                    .startsWith(propertiesStorage.getLocalPushEndpointPattern())
            ) {

                String id = accessor.getSessionAttributes().get(

                ).toString();

                Long userId = (Long) accessor.getSessionAttributes()
                        .get(ConnectSTOMPInterceptor.USER_ID_SESSION_ATTRIBUTE);

                LeaderboardResponse leaderboardPayload = leaderboardWebClient.getLeaderboard(id);

                simpMessagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        ConnectSTOMPInterceptor.INIT_LEADERBOARD_EXPECTATION + id,
                        leaderboardPayload
                );
            }
        }
    }
}