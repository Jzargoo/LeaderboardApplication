package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.exceptions.CannotCreateCachedUserException;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import com.jzargo.messaging.UserUpdateEvent;
import org.springframework.transaction.annotation.Transactional;

public interface LeaderboardService {
    void increaseUserScore(UserScoreEvent changeEvent) throws CannotCreateCachedUserException;
    void addNewScore(UserScoreUploadEvent uploadEvent);
    String createLeaderboard(
            InitLeaderboardCreateEvent initLeaderboardCreateEvent,
            String region);
    void initUserScore(
            InitUserScoreRequest request,
            String username, long userId, String region);
    void updateUserCache(UserUpdateEvent userUpdateEvent);
    void deleteLeaderboard(String lbId, String sagaId);
    void confirmLbCreation(String lbId);
}
