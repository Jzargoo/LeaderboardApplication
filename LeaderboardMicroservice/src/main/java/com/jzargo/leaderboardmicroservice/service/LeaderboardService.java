package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;

public interface LeaderboardService {
    void increaseUserScore(UserScoreEvent changeEvent, long userId, String name, String region);
    void addNewScore(UserScoreUploadEvent uploadEvent, long ownerId, String username, String region);
    void createLeaderboard(CreateLeaderboardRequest request, long ownerId, String username, String region);
}
