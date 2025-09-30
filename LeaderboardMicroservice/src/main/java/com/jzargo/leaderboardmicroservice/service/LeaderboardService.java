package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;

public interface LeaderboardService {
    void increaseUserScore(UserScoreEvent ChangeEvent);
    void createLeaderboard(CreateLeaderboardRequest request, long userId);
    void addNewScore(UserScoreUploadEvent uploadEvent);
}
