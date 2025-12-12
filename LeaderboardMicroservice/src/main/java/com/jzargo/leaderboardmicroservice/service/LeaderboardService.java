package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.exceptions.CannotCreateCachedUserException;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;

public interface LeaderboardService {
    void increaseUserScore(UserScoreEvent changeEvent) throws CannotCreateCachedUserException;
    void addNewScore(UserScoreUploadEvent uploadEvent);
    String createLeaderboard(
            InitLeaderboardCreateEvent initLeaderboardCreateEvent,
            String region);
    void initUserScore(
            InitUserScoreRequest request,
            long userId);
    void deleteLeaderboard(String lbId, String sagaId);
    void confirmLbCreation(String lbId);
    LeaderboardResponse getLeaderboard(String id);
    boolean userExistsById(Long id, String ldId);
}
