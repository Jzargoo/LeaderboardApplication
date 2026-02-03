package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.messaging.LeaderboardEventDeletion;
import com.jzargo.messaging.LeaderboardEventInitialization;

public interface ScoringServiceWebProxy {
    void initiateEvents(LeaderboardEventInitialization leaderboardEventInitialization, String sagaId);
    void deleteLeaderboardEvents(LeaderboardEventDeletion leaderboardEventDeletion, String sagaId);
    default TypesOfProxy getType() {
        return TypesOfProxy.KAFKA;
    }
}
