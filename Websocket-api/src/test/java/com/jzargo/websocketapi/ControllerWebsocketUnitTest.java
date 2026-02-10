package com.jzargo.websocketapi;

import com.jzargo.websocketapi.controller.WebSocketController;
import com.jzargo.websocketapi.dto.LeaderboardPushEvent;
import com.jzargo.websocketapi.dto.PushEventType;
import com.jzargo.websocketapi.service.WebSocketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class ControllerWebsocketUnitTest {
        @Mock
        private WebSocketService webSocketService;

        @InjectMocks
        private WebSocketController controller;

        @Test
        void shouldCallUpdateScore() {

            LeaderboardPushEvent<Double> event =
                    new LeaderboardPushEvent<>(
                            PushEventType.UPDATE_SCORE,
                            100.0
                    );

            controller.pushScore("lb1", event, 1L);

            verify(webSocketService).updateUserScore(
                    eq("lb1"),
                    any(LeaderboardPushEvent.UpdateLeaderboardScore.class)
            );
        }

        @Test
        void shouldCallIncreaseScore() {

            LeaderboardPushEvent<String> event =
                    new LeaderboardPushEvent<>(
                            PushEventType.INCREASE_SCORE,
                            "10"
                    );

            controller.pushScore("lb1", event, 1L);

            verify(webSocketService).increaseUserScore(
                    eq("lb1"),
                    any(LeaderboardPushEvent.IncreaseUserScore.class)
            );
        }

        @Test
        void shouldNotCallServiceWhenUserIdZero() {

            LeaderboardPushEvent<Double> event =
                    new LeaderboardPushEvent<>(
                            PushEventType.UPDATE_SCORE,
                            100.0
                    );

            controller.pushScore("lb1", event, 0L);

            verifyNoInteractions(webSocketService);
        }
}
