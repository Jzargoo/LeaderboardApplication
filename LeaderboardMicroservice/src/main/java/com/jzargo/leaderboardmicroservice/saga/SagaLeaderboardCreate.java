package com.jzargo.leaderboardmicroservice.saga;

import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.messaging.*;

public interface SagaLeaderboardCreate {
    void startSaga(CreateLeaderboardRequest request, long userId, String username);

    boolean stepCreateLeaderboard(InitLeaderboardCreateEvent event, String SagaId);
    void stepSuccessfulEventInit(SuccessfulEventInitialization successfulEventInitialization, String sagaId);
    void stepSagaCompleted(UserAddedLeaderboard userAddedLeaderboard, String sagaId);
    void compensateStepUserProfile(
            String sagaId,
            FailedLeaderboardCreation failedLeaderboardCreation
    );
    void compensateStepOptionalEvent(
            String sagaId,
            String lbId
    );

    void stepCompensateLeaderboard(DeleteLbEvent dle, String sagaId);
    boolean stepOutOfTime(String lbId);
}
