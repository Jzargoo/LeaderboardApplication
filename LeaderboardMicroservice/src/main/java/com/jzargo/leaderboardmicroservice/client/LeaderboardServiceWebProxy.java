package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.messaging.DeleteLbEvent;
import com.jzargo.messaging.OutOfTimeEvent;

public interface LeaderboardServiceWebProxy {

    void outOfTime(OutOfTimeEvent event, String sagaId);
    void createLeaderboard(InitLeaderboardCreateEvent event, String sagaId);
    default TypesOfProxy getType() {
        return TypesOfProxy.KAFKA;
    }

    void compensateLeaderboard(DeleteLbEvent deleteLbEvent, String sagaId);
}
