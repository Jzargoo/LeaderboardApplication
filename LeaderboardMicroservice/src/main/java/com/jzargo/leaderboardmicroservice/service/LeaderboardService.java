package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;

public interface LeaderboardService {
    void increaseUserScore(UserScoreEvent changeEvent);
    void addNewScore(UserScoreUploadEvent uploadEvent);
    void createLeaderboard(CreateLeaderboardRequest request, long ownerId, String username, String region);
    void initUserScore(InitUserScoreRequest request, String username, long userId, String region);
}
