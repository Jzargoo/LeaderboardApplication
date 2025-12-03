package com.jzargo.websocketapi.handler;

import com.jzargo.messaging.UserLocalUpdateEvent;
import com.jzargo.websocketapi.config.KafkaConfig;
import com.jzargo.websocketapi.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KafkaListener(
        topics = KafkaConfig.LEADERBOARD_UPDATE_TOPIC,
        groupId = KafkaConfig.GROUP_ID
)
public class KafkaLeaderboardReceivedHandler {

    private final WebSocketService webSocketService;

    public KafkaLeaderboardReceivedHandler(WebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @KafkaHandler
    public void handleLocalLeaderboardUpdate(@Payload UserLocalUpdateEvent userLocalUpdateEvent){
        log.debug("Received UserLocalUpdateEvent for lb id: {}", userLocalUpdateEvent.getLeaderboardId());
        webSocketService.refreshLocalLeaderboard(userLocalUpdateEvent);
    }
}
