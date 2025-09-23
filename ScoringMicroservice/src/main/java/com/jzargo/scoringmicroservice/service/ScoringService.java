package com.jzargo.scoringmicroservice.service;

import messaging.LeaderboardEventDeletion;
import messaging.LeaderboardEventInitialization;
import messaging.UserEventHappenedCommand;

public interface ScoringService {
    void saveUserEvent(UserEventHappenedCommand message);
    void saveEvents(LeaderboardEventInitialization message);
    boolean deleteEvents(LeaderboardEventDeletion message);
}
