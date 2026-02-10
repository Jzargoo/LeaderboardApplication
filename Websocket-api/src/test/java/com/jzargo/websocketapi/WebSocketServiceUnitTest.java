package com.jzargo.websocketapi;

import com.jzargo.messaging.GlobalLeaderboardEvent;
import com.jzargo.messaging.UserLocalUpdateEvent;
import com.jzargo.websocketapi.config.properties.ApplicationPropertiesStorage;
import com.jzargo.websocketapi.dto.LeaderboardResponsePayload;
import com.jzargo.websocketapi.service.WebSocketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class WebSocketServiceUnitTest {
    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private ApplicationPropertiesStorage applicationPropertiesStorage;

    @InjectMocks
    private WebSocketServiceImpl webSocketService;

    @Test
    void refreshLocalLeaderboard_shouldSendToEachUser() {

        UserLocalUpdateEvent event = new UserLocalUpdateEvent(
                "777",
                List.of(
                        new UserLocalUpdateEvent.UserLocalEntry(1L, 100.0, 5L),
                        new UserLocalUpdateEvent.UserLocalEntry(2L, 200.0, 1L)
                )
        );

        webSocketService.refreshLocalLeaderboard(event);

        verify(simpMessagingTemplate).convertAndSendToUser(
                eq("1"),
                eq(applicationPropertiesStorage.getEndpointsPattern().getLocalLeaderboardPush()),
                argThat(object ->{
                    LeaderboardResponsePayload payload = (LeaderboardResponsePayload) object;
                    return "777".equals(payload.getLeaderboardId()) &&
                            payload.getUserId() == 1L &&
                            payload.getScore() == 100 &&
                            payload.getRank() == 5;
                })
        );

        verify(simpMessagingTemplate).convertAndSendToUser(
                eq("2"),
                eq(applicationPropertiesStorage.getEndpointsPattern().getGlobalLeaderboardPush()),
                argThat(object->{
                    LeaderboardResponsePayload payload = (LeaderboardResponsePayload) object;
                    return "777".equals(payload.getLeaderboardId()) &&
                                payload.getUserId() == 2L &&
                                payload.getScore() == 200 &&
                                payload.getRank() == 1;
                        }
                )
        );

        verifyNoMoreInteractions(simpMessagingTemplate);
    }
    @Test
    void refreshLocalLeaderboard_withNoEntries_shouldNotSendAnything(){
        UserLocalUpdateEvent event = new UserLocalUpdateEvent(
                "888",
                List.of()
        );

        webSocketService.refreshLocalLeaderboard(event);

        verifyNoMoreInteractions(simpMessagingTemplate);
    }
    @Test
    void refreshLocalLeaderboard_withNullEntries_shouldNotSendAnything(){
        UserLocalUpdateEvent event = new UserLocalUpdateEvent(
                "999",
                null
        );
        webSocketService.refreshLocalLeaderboard(event);
        verifyNoMoreInteractions(simpMessagingTemplate);
    }
    @Test
    void refreshGlobalLeaderboard_shouldDoNothing(){
        webSocketService.refreshGlobalLeaderboard(null);
        verifyNoMoreInteractions(simpMessagingTemplate);
    }
    @Test
    void refreshGlobalLeaderboard_withValidEvent_interact(){
        webSocketService.refreshGlobalLeaderboard(
                new GlobalLeaderboardEvent(
                        "leaderboard-1",
                        LocalDateTime.now(),
                        List.of(
                                new GlobalLeaderboardEvent.Entry(1L, 500.0),
                                new GlobalLeaderboardEvent.Entry(2L, 400.0)
                        )
                )
        );
        verify(simpMessagingTemplate).convertAndSend(
                eq(applicationPropertiesStorage.getEndpointsPattern().getGlobalLeaderboardPush()),
                argThat((LeaderboardResponsePayload payload) -> "leaderboard-1".equals(payload.getLeaderboardId()) &&
                        payload.getUserId() == 1L &&
                        payload.getScore() == 500.0 &&
                        payload.getRank() == 1)
        );
    }
}