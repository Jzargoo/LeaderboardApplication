package com.jzargo.leaderboardmicroservice.saga;

import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;

public interface SagaLeaderboardCreate {
    void startSaga(CreateLeaderboardRequest request, long userId, String username, String region);
}
