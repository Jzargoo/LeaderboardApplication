package com.jzargo.websocketapi.controller;

import com.jzargo.region.Regions;
import com.jzargo.websocketapi.dto.LeaderboardPushEvent;
import com.jzargo.websocketapi.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    private final WebSocketService webSocketService;

    @MessageMapping("/queue/leaderboard-push/{id}")
    public void pushScore(@DestinationVariable String id,
                          @Payload LeaderboardPushEvent<?> lbEvent,
                          @Attribute
    ) {


        if(userId == 0){
            log.error("Invalid user id in JWT token");
            return;
        }

        String region = jwt.getClaimAsString("region") == null?
                Regions.GLOBAL.getCode():
                Regions.fromStringCode(
                        jwt.getClaimAsString("region")).getCode();

        switch (lbEvent.getType()) {

            case UPDATE_SCORE -> {

                LeaderboardPushEvent.UpdateLeaderboardScore updateUserScore =
                        new LeaderboardPushEvent.UpdateLeaderboardScore(
                                userId, (Double) lbEvent.getPayload(), region, preferredUsername
                        );
                webSocketService.updateUserScore(id, updateUserScore);

            }

            case INCREASE_SCORE -> {

                LeaderboardPushEvent.IncreaseUserScore increaseUserScore =
                        new LeaderboardPushEvent.IncreaseUserScore(
                                userId, (String) lbEvent.getPayload(), region, preferredUsername
                        );
                webSocketService.increaseUserScore(id, increaseUserScore);

            }

            default -> log.error("Illegal leaderboard event type: {}", lbEvent.getType());
        }
    }

}
