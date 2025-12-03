package com.jzargo.websocketapi.lifecylce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.websocketapi.service.LeaderboardWebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardJoinEventListener {

    private final LeaderboardWebClient leaderboardWebClient;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handleConnectRequest(SessionConnectedEvent event) throws JsonProcessingException {
        StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String lbId = (String) Objects.requireNonNull(stompHeaderAccessor
                        .getSessionAttributes())
                .get(CheckLeaderboardIdInterceptor.LEADERBOARD_ATTRIBUTE);

        LeaderboardResponse leaderboard = leaderboardWebClient.getLeaderboard(lbId);

        simpMessagingTemplate.convertAndSendToUser(
                Objects.requireNonNull(
                        stompHeaderAccessor.getSessionId()
                ),
                "/queue/connected",
                objectMapper.writeValueAsString(leaderboard)
        );

        log.info("Sent leaderboard data to user for leaderboard id: {}", lbId);
    }
}