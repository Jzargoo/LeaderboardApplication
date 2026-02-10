package com.jzargo.websocketapi.lifecylce;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.dto.UserScoreResponse;
import com.jzargo.websocketapi.config.properties.ApplicationPropertiesStorage;
import com.jzargo.websocketapi.dto.LeaderboardRefreshResponse;
import com.jzargo.websocketapi.exception.ParticipantException;
import com.jzargo.websocketapi.mapper.CreateLeaderboardRefreshRequestMapper;
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
    private final ApplicationPropertiesStorage applicationPropertiesStorage;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final CreateLeaderboardRefreshRequestMapper createLeaderboardRefreshRequestMapper;

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
                                        applicationPropertiesStorage.getEndpointsPattern().getLocalLeaderboardPush()
                                ) ||
                                        accessor.getDestination().startsWith(
                                                applicationPropertiesStorage.getEndpointsPattern().getGlobalLeaderboardPush()
                                        )
                        )
        ) {

            String lbId = (String) accessor.getSessionAttributes()
                    .get(applicationPropertiesStorage.getAttribute().getLeaderboardId());
            long userId = (Long) accessor.getSessionAttributes()
                    .get(applicationPropertiesStorage.getAttribute().getUserId());

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

        //if accessor does not have destination, it will not be a valid message,
        //so message should be stopped by throwing an exception
        if (accessor.getDestination() ==null) {
            throw new NullArgumentException("Destination in accessor for subscribe stomp interceptor is null");
        }

        if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
            if(accessor.getDestination()
                    .startsWith(applicationPropertiesStorage.getEndpointsPattern().getLocalLeaderboardPush())
            ) {

                String lbId = accessor.getSessionAttributes()
                        .get(applicationPropertiesStorage.getAttribute().getLeaderboardId())
                        .toString();

                Long userId = (Long) accessor.getSessionAttributes()
                        .get(applicationPropertiesStorage.getAttribute().getUserId());

                UserScoreResponse usr =
                        leaderboardWebClient.myScoreIn(lbId, userId);
                LeaderboardResponse leaderboard =
                        leaderboardWebClient.getLeaderboard(lbId);

                LeaderboardRefreshResponse map = createLeaderboardRefreshRequestMapper.map(
                        new CreateLeaderboardRefreshRequestMapper.RefreshInfo(
                                usr, leaderboard
                        )
                );

                simpMessagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        applicationPropertiesStorage.getEndpointsPattern().getLocalLeaderboardPush() + lbId,
                        map
                );


            }
        }
    }
}