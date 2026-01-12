package com.jzargo.websocketapi.lifecylce;

import com.jzargo.dto.UserScoreResponse;
import com.jzargo.websocketapi.exception.ParticipantException;
import com.jzargo.websocketapi.service.LeaderboardWebClient;
import com.jzargo.websocketapi.utils.PropertiesStorage;
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

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if(!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        if (
                accessor.getDestination() != null &&
                        (
                                accessor.getDestination().startsWith(
                                        propertiesStorage.getEndpointsPattern().getLocalLeaderboardPush()
                                ) ||
                                        accessor.getDestination().startsWith(
                                                propertiesStorage.getEndpointsPattern().getGlobalLeaderboardPush()
                                        )
                        )
        ) {

            String lbId = (String) accessor.getSessionAttributes()
                    .get(propertiesStorage.getAttribute().getLeaderboardId());
            long userId = (Long) accessor.getSessionAttributes()
                    .get(propertiesStorage.getAttribute().getUserId());

            boolean participant = leaderboardWebClient.isParticipant(lbId, userId + "");

            if (!participant) {
                throw new ParticipantException("user is not a part of the leaderboard and cannot subscribe on it");
            }
        }
        return message;
    }


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
                    .startsWith(propertiesStorage.getEndpointsPattern().getLocalLeaderboardPush())
            ) {

                String lbId = accessor.getSessionAttributes()
                        .get(propertiesStorage.getAttribute().getLeaderboardId())
                        .toString();

                Long userId = (Long) accessor.getSessionAttributes()
                        .get(propertiesStorage.getAttribute().getUserId());

                UserScoreResponse usr =
                        leaderboardWebClient.myScoreIn(lbId, userId);
                simpMessagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        propertiesStorage.getEndpointsPattern().getLocalLeaderboardPush() + lbId,
                        usr
                );
            }
        }
    }
}