package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardServiceImpl implements LeaderboardService{

    private final StringRedisTemplate stringRedisTemplate;
    private final LeaderboardInfoRepository leaderboardInfoRepository;
    private final RedisScript<String> mutableLeaderboardScript;
    private final RedisScript<String> immutableLeaderboardScript;

    public LeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate, LeaderboardInfoRepository LeaderboardInfoRepository,
                                  RedisScript<String> mutableLeaderboardScript, RedisScript<String> immutableLeaderboardScript
                                  ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.leaderboardInfoRepository = LeaderboardInfoRepository;
        this.mutableLeaderboardScript = mutableLeaderboardScript;
        this.immutableLeaderboardScript = immutableLeaderboardScript;
    }

    @Override
    public void increaseUserScore(UserScoreEvent changeEvent) {
        executeScoreChange(
                changeEvent.getLbId(),
                changeEvent.getUserId(),
                changeEvent.getScore(),
                true
        );
    }

    @Override
    public void addNewScore(UserScoreUploadEvent uploadEvent) {
        executeScoreChange(

                uploadEvent.getLbId(),
                uploadEvent.getUserId(),
                uploadEvent.getScore(),
                false
        );
    }

    private void executeScoreChange(String lbId, Long userId, double scoreDelta, boolean isMutable) {
        LeaderboardInfo info = leaderboardInfoRepository.findById(lbId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Leaderboard with id " + lbId + " does not exist")
                );

        List<String> keys = getStrings(userId, lbId, info.getGlobalRange());

        String execute = stringRedisTemplate.execute(
                isMutable? mutableLeaderboardScript : immutableLeaderboardScript,
                keys,
                userId.toString(),
                String.valueOf(scoreDelta),
                String.valueOf(info.getMaxEventsPerUser()),
                String.valueOf(info.getMaxEventsPerUserPerDay()),
                String.join(",", info.getRegions()),
                info.isPublic() ? "1" : "0",
                String.valueOf(info.getGlobalRange())
        );

        if (!"success".equals(execute)) {
            throw new IllegalStateException("Failed to update score for user " + userId + ": " + execute);
        }
    }

    private static List<String> getStrings(Long userId, String lbId, int globalRange) {
        String daily = "user_cached:" + userId + ":dailyAttempts";
        String ttla = "user_cached:" + userId + ":totalAttempts";
        String lbk = "leaderboard:" + lbId + ":mutable";
        String uhk = "user_cached:" + userId;
        String globalLbk = "leaderboard-cache:" + lbId + ":top" + globalRange + "Leaderboard";
        String localLbk = "leaderboard-cache:" + lbId + ":userId:" + userId + ":local-leaderboard-update";

        return List.of(daily, ttla, lbk, uhk, globalLbk, localLbk);
    }
}