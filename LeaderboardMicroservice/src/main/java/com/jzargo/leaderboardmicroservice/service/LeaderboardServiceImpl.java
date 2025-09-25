package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import messaging.UserScoreEvent;
import messaging.UserScoreUploadEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardServiceImpl implements LeaderboardService{

    private final StringRedisTemplate stringRedisTemplate;
    private final LeaderboardInfoRepository leaderboardInfoRepository;
    private final RedisScript<String> leaderboardScript;

    public LeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate, LeaderboardInfoRepository LeaderboardInfoRepository,
                                   RedisScript<String> leaderboardScript) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.leaderboardInfoRepository = LeaderboardInfoRepository;
        this.leaderboardScript = leaderboardScript;
    }

    @Override
    public void increaseUserScore(UserScoreEvent changeEvent) {
        LeaderboardInfo info = leaderboardInfoRepository.findById(changeEvent.getLbId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Leaderboard with id " + changeEvent.getLbId() + " does not exist")
                );

        String daily = "user_cached:" + changeEvent.getUserId() + ":dailyAttempts";
        String  ttla = "user_cached:" + changeEvent.getUserId() + ":totalAttempts";
        String   lbk = "leaderboard:" + changeEvent.getLbId() + ":mutable";
        String   uhk = "user_cached:" + changeEvent.getUserId();

        List<String> keys = List.of(daily, ttla, lbk, uhk);

        String userId = changeEvent.getUserId().toString();
        String scoreDelta = ""+changeEvent.getScore();
        String maxEventsTotal = String.valueOf(info.getMaxEventsPerUser());
        String maxEventsDaily = String.valueOf(info.getMaxEventsPerUserPerDay());

        String execute = stringRedisTemplate.execute(
                leaderboardScript,
                keys,
                userId, scoreDelta, maxEventsTotal, maxEventsDaily
        );

        if(execute.equals("ERR")) {
            throw new IllegalStateException("Failed to update score for user " + changeEvent.getUserId());
        }
    }
    @Override
    public void addNewScore( UserScoreUploadEvent uploadEvent) {

    }
}
