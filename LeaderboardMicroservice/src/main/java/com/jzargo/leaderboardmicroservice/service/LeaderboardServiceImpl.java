package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import messaging.UserScoreEvent;
import messaging.UserScoreUploadEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LeaderboardServiceImpl implements LeaderboardService{

    private final StringRedisTemplate stringRedisTemplate;
    private final LeaderboardInfoRepository LeaderboardInfoRepository;

    public LeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate, LeaderboardInfoRepository LeaderboardInfoRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.LeaderboardInfoRepository = LeaderboardInfoRepository;
    }

    @Override
    public void increaseUserScore(UserScoreEvent changeEvent) {
        if (!LeaderboardInfoRepository.existsById(changeEvent.getLbId())) {
            throw new IllegalArgumentException
                    ();
        }
        LeaderboardInfo info = LeaderboardInfoRepository.findById(changeEvent.getLbId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Leaderboard with id " + changeEvent.getLbId() + " does not exist")
                );
        if(info.)


    }

    @Override
    public void addNewScore(UserScoreUploadEvent uploadEvent) {

    }
}
