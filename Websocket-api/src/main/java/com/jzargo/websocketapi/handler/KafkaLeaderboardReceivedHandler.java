package com.jzargo.websocketapi.handler;

import com.jzargo.messaging.GlobalLeaderboardEvent;
import com.jzargo.messaging.UserLocalUpdateEvent;
import com.jzargo.websocketapi.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KafkaListener(
        topics = "#{@kafkaPropertiesStorage.topic.names.leaderboardEvent}",
        groupId = "#{@kafkaPropertiesStorage.consumer.groupId}"

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

    @KafkaHandler
    public void handleGlobalLeaderboardEvent(@Payload GlobalLeaderboardEvent gle) {
        log.debug("Received Global leaderboard event for lb id: {}", gle.getId());
        webSocketService.refreshGlobalLeaderboard(gle);
    }
}
