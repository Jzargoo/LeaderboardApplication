package com.jzargo.websocketapi.controller;

import com.jzargo.websocketapi.dto.LeaderboardPushEvent;
import com.jzargo.websocketapi.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.SessionAttribute;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    private final WebSocketService webSocketService;

    @MessageMapping("/queue/leaderboard-push/{id}")
    public void pushScore(@DestinationVariable String id,
                          @Payload LeaderboardPushEvent<?> lbEvent,
                          @SessionAttribute long userId
    ) {


        if(userId == 0){
            log.error("Invalid session because of userId session attribute");
            return;
        }


        switch (lbEvent.getType()) {

            case UPDATE_SCORE -> {

                LeaderboardPushEvent.UpdateLeaderboardScore updateUserScore =
                        new LeaderboardPushEvent.UpdateLeaderboardScore(
                                userId, (Double) lbEvent.getPayload()
                        );
                webSocketService.updateUserScore(id, updateUserScore);

            }

            case INCREASE_SCORE -> {

                LeaderboardPushEvent.IncreaseUserScore increaseUserScore =
                        new LeaderboardPushEvent.IncreaseUserScore(
                                userId, (String) lbEvent.getPayload()
                        );
                webSocketService.increaseUserScore(id, increaseUserScore);

            }

            default -> log.error("Illegal leaderboard event type: {}", lbEvent.getType());
        }
    }

}
