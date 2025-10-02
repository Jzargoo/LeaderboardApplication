package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.mapper.MapperCreateLeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.CachedUserRepository;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService{

    private final StringRedisTemplate stringRedisTemplate;
    private final LeaderboardInfoRepository leaderboardInfoRepository;
    private final RedisScript<String> mutableLeaderboardScript;
    private final RedisScript<String> immutableLeaderboardScript;
    private final RedisScript<String> createLeaderboardScript;
    private final MapperCreateLeaderboardInfo mapperCreateLeaderboardInfo;
    private final CachedUserRepository cachedUserRepository;
    private final RedisScript<String> createUserCachedScript;

    public LeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate, LeaderboardInfoRepository LeaderboardInfoRepository,
                                  RedisScript<String> mutableLeaderboardScript, RedisScript<String> immutableLeaderboardScript,
                                  MapperCreateLeaderboardInfo mapperCreateLeaderboardInfo, RedisScript<String> createLeaderboardScript,
                                  CachedUserRepository cachedUserRepository, RedisScript<String> createUserCachedScript) {

        this.stringRedisTemplate = stringRedisTemplate;
        this.leaderboardInfoRepository = LeaderboardInfoRepository;
        this.mutableLeaderboardScript = mutableLeaderboardScript;
        this.immutableLeaderboardScript = immutableLeaderboardScript;
        this.mapperCreateLeaderboardInfo = mapperCreateLeaderboardInfo;
        this.createLeaderboardScript = createLeaderboardScript;
        this.cachedUserRepository = cachedUserRepository;
        this.createUserCachedScript = createUserCachedScript;
    }

    @Override
    public void increaseUserScore(UserScoreEvent changeEvent) {
        userCachedCheck(changeEvent.getUserId(), changeEvent.getName(), changeEvent.getRegion());
        executeScoreChange(
                changeEvent.getLbId(),
                changeEvent.getUserId(),
                changeEvent.getScore(),
                true
        );
        log.info("incremented score for user: " + changeEvent.getUserId());
    }

    private void userCachedCheck(Long userId, String username, String region) {
        if (cachedUserRepository.existsById(userId)) {
            return;
        }

        String execute = stringRedisTemplate.execute(createUserCachedScript,
                List.of("user_cached:" + userId,
                        "user_cached:" + userId + ":daily_attempts",
                        "user_cached:" + userId + ":total_attempts"
                ),
                userId, username, region
        );
        if(!execute.equals("OK")) {
            log.error("Adding new user failed with id {} and name {}", userId, username);
            throw new IllegalStateException("Cannot create user cached version");
        }
        log.info("Adding new user ended successfully for username {}",username);
    }

    @Override
    public void addNewScore(UserScoreUploadEvent uploadEvent) {
        userCachedCheck(uploadEvent.getUserId(),uploadEvent.getName(), "un");
        executeScoreChange(

                uploadEvent.getLbId(),
                uploadEvent.getUserId(),
                uploadEvent.getScore(),
                false
        );
        log.info("added score for user: " + uploadEvent.getUserId());
    }

    private void executeScoreChange(String lbId, Long userId, double scoreDelta, boolean isMutable) {
        LeaderboardInfo info = leaderboardInfoRepository.findById(lbId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Leaderboard with id " + lbId + " does not exist")
                );

        List<String> keys = getStrings(userId, lbId, info.getGlobalRange(), isMutable);

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

    private static List<String> getStrings(Long userId, String lbId, int globalRange, boolean isMutable) {
        String daily = "user_cached:" + userId + ":dailyAttempts";
        String ttla = "user_cached:" + userId + ":totalAttempts";

        String lbk = "leaderboard:" + lbId + (isMutable?
                ":mutable":
                ":immutable");

        String uhk = "user_cached:" + userId;
        String globalLbk = "leaderboard-cache:" + lbId + ":top" + globalRange + "Leaderboard";
        String localLbk = "leaderboard-cache:" + lbId + ":userId:" + userId + ":local-leaderboard-update";

        return List.of(daily, ttla, lbk, uhk, globalLbk, localLbk);
    }

    @Override
    public void createLeaderboard(CreateLeaderboardRequest request, long ownerId) {
        if(request.getMaxScore() < -1 && request.getMaxScore() == request.getInitialValue()) {
            throw new IllegalArgumentException("initial value cannot be equal or greater than max score");
        }
        request.setOwnerId(ownerId);
        LeaderboardInfo map = mapperCreateLeaderboardInfo.map(request);

        StringBuilder builder = new StringBuilder("leaderboard:");
        builder.append(map.getId())
                .append(":")
                .append(map.isMutable()?
                        "mutable":
                        "immutable");

        String id = builder.toString();

        List<String> keys = List.of(
                id,
                "leaderboard_information:" + map.getId()
        );

        stringRedisTemplate.execute(createLeaderboardScript,
                keys,
                ownerId, request.getInitialValue(),
                map.getId(), request.getDescription(),
                map.isPublic(),map.isMutable(), map.isShowTies()
        );
    }
}