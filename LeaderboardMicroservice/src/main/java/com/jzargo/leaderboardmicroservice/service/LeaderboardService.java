package com.jzargo.leaderboardmicroservice.service;

import messaging.UserScoreEvent;
import messaging.UserScoreUploadEvent;

public interface LeaderboardService {
    void increaseUserScore(UserScoreEvent ChangeEvent);
    void addNewScore(UserScoreUploadEvent uploadEvent);
}
