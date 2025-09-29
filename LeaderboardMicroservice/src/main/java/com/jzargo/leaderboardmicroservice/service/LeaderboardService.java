package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;

public interface LeaderboardService {
    void increaseUserScore(UserScoreEvent ChangeEvent);
    void addNewScore(UserScoreUploadEvent uploadEvent);
}
