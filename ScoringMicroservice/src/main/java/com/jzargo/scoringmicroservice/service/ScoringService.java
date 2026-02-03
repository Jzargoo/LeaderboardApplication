package com.jzargo.scoringmicroservice.service;

import com.jzargo.messaging.LeaderboardEventDeletion;
import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.UserEventHappenedCommand;

public interface ScoringService {
    void saveUserEvent(UserEventHappenedCommand message);
    void saveEvents(LeaderboardEventInitialization message);
    boolean deleteEvents(LeaderboardEventDeletion message);
}
