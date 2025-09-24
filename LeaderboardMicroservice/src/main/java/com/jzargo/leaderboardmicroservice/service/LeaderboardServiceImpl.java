package com.jzargo.leaderboardmicroservice.service;

import messaging.UserScoreEvent;
import messaging.UserScoreUploadEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardServiceImpl implements LeaderboardService{

    private final StringRedisTemplate stringRedisTemplate;

    public LeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void increaseUserScore(UserScoreEvent ChangeEvent) {
        stringRedisTemplate
    }

    @Override
    public void addNewScore(UserScoreUploadEvent uploadEvent) {

    }
}
