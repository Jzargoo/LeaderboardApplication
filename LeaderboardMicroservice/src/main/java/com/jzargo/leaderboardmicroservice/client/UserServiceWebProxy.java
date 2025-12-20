package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.messaging.UserNewLeaderboardCreated;

public interface UserServiceWebProxy {

    void createUserAddedLeaderboard(
            UserNewLeaderboardCreated userNewLeaderboardCreated, String sagaId
    );


    default TypesOfProxy getType() {
        return TypesOfProxy.KAFKA;
    }
}
