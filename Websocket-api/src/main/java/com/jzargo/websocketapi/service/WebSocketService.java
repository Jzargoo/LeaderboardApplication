package com.jzargo.websocketapi.service;

import com.jzargo.messaging.GlobalLeaderboardEvent;
import com.jzargo.messaging.UserLocalUpdateEvent;
import com.jzargo.websocketapi.dto.LeaderboardPushEvent;

public interface WebSocketService {
    void initLeaderboardScore(String id);

    void updateUserScore(String id, LeaderboardPushEvent.UpdateLeaderboardScore updateUserScore);

    void increaseUserScore(String id, LeaderboardPushEvent.IncreaseUserScore increaseUserScore);

    void refreshLocalLeaderboard(UserLocalUpdateEvent userLocalUpdateEvent);
    void refreshGlobalLeaderboard(GlobalLeaderboardEvent globalLeaderboardEvent);
}
