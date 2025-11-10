package com.jzargo.leaderboardmicroservice.saga;

import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.messaging.FailedLeaderboardCreation;
import com.jzargo.messaging.SuccessfulEventInitialization;
import com.jzargo.messaging.UserAddedLeaderboard;

public interface SagaLeaderboardCreate {
    void startSaga(CreateLeaderboardRequest request, long userId, String username, String region);

    boolean stepCreateLeaderboard(InitLeaderboardCreateEvent event, String region, String SagaId);
    void stepSuccessfulEventInit(SuccessfulEventInitialization successfulEventInitialization, String sagaId);
    void stepSagaCompleted(UserAddedLeaderboard userAddedLeaderboard, String sagaId);

    void compensateStepUserProfile(
            String sagaId,
            FailedLeaderboardCreation failedLeaderboardCreation
    );

    void compensateStepOptionalEvent(
            String sagaId,
            FailedLeaderboardCreation failedLeaderboardCreation
    );
}
