package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.leaderboardmicroservice.config.RedisConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.UserCached;
import com.jzargo.leaderboardmicroservice.mapper.MapperCreateLeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.CachedUserRepository;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import com.jzargo.messaging.UserUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    private final MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;
    private final RedisScript<String> deleteLeaderboardScript;

    public LeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate, LeaderboardInfoRepository LeaderboardInfoRepository,
                                  RedisScript<String> mutableLeaderboardScript, RedisScript<String> immutableLeaderboardScript,
                                  MapperCreateLeaderboardInfo mapperCreateLeaderboardInfo, RedisScript<String> createLeaderboardScript,
                                  CachedUserRepository cachedUserRepository, RedisScript<String> createUserCachedScript, MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter, RedisScript<String> deleteLeaderboardScript) {

        this.stringRedisTemplate = stringRedisTemplate;
        this.leaderboardInfoRepository = LeaderboardInfoRepository;
        this.mutableLeaderboardScript = mutableLeaderboardScript;
        this.immutableLeaderboardScript = immutableLeaderboardScript;
        this.mapperCreateLeaderboardInfo = mapperCreateLeaderboardInfo;
        this.createLeaderboardScript = createLeaderboardScript;
        this.cachedUserRepository = cachedUserRepository;
        this.createUserCachedScript = createUserCachedScript;
        this.mappingJackson2HttpMessageConverter = mappingJackson2HttpMessageConverter;
        this.deleteLeaderboardScript = deleteLeaderboardScript;
    }

    @Override
    @Transactional
    public void increaseUserScore(UserScoreEvent changeEvent) {

        userCachedCheck(changeEvent.getUserId(), changeEvent.getUsername(), changeEvent.getRegion());
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
                userId.toString(), username, region
        );
        if(!execute.equals("OK")) {
            log.error("Adding new user failed with id {} and name {}", userId, username);
            throw new IllegalStateException("Cannot create user cached version");
        }
        log.info("Adding new user ended successfully for username {}",username);
    }

    @Override
    @Transactional
    public void addNewScore(UserScoreUploadEvent uploadEvent) {
        userCachedCheck(uploadEvent.getUserId(), uploadEvent.getUsername(), uploadEvent.getRegion());
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

        UserCached cached = cachedUserRepository.findById(userId).orElseThrow(() ->
                        new IllegalArgumentException("Leaderboard with id " + lbId + " does not exist")
        );

        List<String> keys = getStrings(userId, lbId, info.getGlobalRange(), isMutable);
        System.out.println("KEYS: " + keys);
        System.out.println(userId +
                String.valueOf(scoreDelta)+
                info.getMaxEventsPerUser() +
                info.getMaxEventsPerUserPerDay() +
                cached.getRegion()+
                info.getGlobalRange()
        );
        String execute = stringRedisTemplate.execute(
                isMutable? mutableLeaderboardScript : immutableLeaderboardScript,
                keys,
                userId.toString(),
                String.valueOf(scoreDelta),
                String.valueOf(info.getMaxEventsPerUser()),
                String.valueOf(info.getMaxEventsPerUserPerDay()),
                info.getRegions().toString(),
                String.valueOf(info.getGlobalRange()),
                lbId,
                cached.getRegion()
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
        String globalLbk = RedisConfig.GLOBAL_STREAM_KEY;
        String localLbk = RedisConfig.LOCAL_STREAM_KEY;

        return List.of(daily, ttla, lbk, uhk, globalLbk, localLbk);
    }

    @Override
    @Transactional
    public String createLeaderboard(InitLeaderboardCreateEvent request, String region) {
        if(request.getMaxScore() < -1 && request.getMaxScore() == request.getInitialValue()) {
            throw new IllegalArgumentException("initial value cannot be equal or greater than max score");
        }
        userCachedCheck(request.getOwnerId(), request.getUsername(), region);
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
                "leaderboard_information:" + map.getId(),
                "leaderboard_signal:" + map.getId()
        );

        stringRedisTemplate.execute(createLeaderboardScript,
                keys,
                String.valueOf(map.getOwnerId()),
                String.valueOf(map.getInitialValue()),
                map.getId(), map.getDescription(),
                String.valueOf(map.isPublic()),
                String.valueOf(map.isMutable()),
                String.valueOf(map.isShowTies()),
                String.valueOf(map.getGlobalRange()),
                LocalDateTime.now().toString(),
                map.getExpireAt().toString(),
                String.valueOf(map.getMaxScore()),
                map.getRegions().toString(),
                String.valueOf(map.getMaxEventsPerUser()),
                String.valueOf(map.getMaxEventsPerUserPerDay()),
                Duration.ofDays(1).toMillis()
        );
        return map.getId();
    }

    @Override
    @Transactional
    public void initUserScore(InitUserScoreRequest request, String username, long userId, String region) {
        LeaderboardInfo lb = leaderboardInfoRepository.findById(request.getLeaderboardId())
                .orElseThrow();
        userCachedCheck(userId, username, region);
        stringRedisTemplate.opsForZSet().add(lb.getKey(),
                String.valueOf(userId),
                lb.getInitialValue()
        );
    }


    @Override
    @Transactional
    public void updateUserCache(UserUpdateEvent userUpdateEvent) {
        UserCached userCached = cachedUserRepository.findById(userUpdateEvent.getId()).orElseThrow();
        userCached.setUsername(userUpdateEvent.getName());
        userCached.setRegion(
                userUpdateEvent
                        .getRegion().getCode());
        cachedUserRepository.save(userCached);
    }

    @Override
    @Transactional
    public void deleteLeaderboard(String lbId) {
        LeaderboardInfo byId =
                leaderboardInfoRepository.findById(lbId).orElseThrow();
        stringRedisTemplate.execute(deleteLeaderboardScript,
                List.of(
                        byId.getKey(),
                        byId.getInfoKey(),
                        byId.getSignalKey())
        );
    }

    @Transactional
    @Override
    public void confirmLbCreation() {

    }
}