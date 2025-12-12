package com.jzargo.leaderboardmicroservice.service;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.leaderboardmicroservice.config.RedisConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.exceptions.CannotCreateCachedUserException;
import com.jzargo.leaderboardmicroservice.mapper.LeaderboardToResponseReadMapper;
import com.jzargo.leaderboardmicroservice.mapper.MapperCreateLeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.CachedUserRepository;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class LeaderboardServiceImpl implements LeaderboardService{

    private final StringRedisTemplate stringRedisTemplate;
    private final LeaderboardInfoRepository leaderboardInfoRepository;
    private final RedisScript<String> mutableLeaderboardScript;
    private final RedisScript<String> immutableLeaderboardScript;
    private final RedisScript<String> createLeaderboardScript;
    private final MapperCreateLeaderboardInfo mapperCreateLeaderboardInfo;
    private final CachedUserRepository cachedUserRepository;
    private final RedisScript<String> createUserCachedScript;
    private final RedisScript<String> deleteLeaderboardScript;
    private final RedisScript<String> confirmLbCreationScript;
    private final SagaControllingStateRepository sagaControllingStateRepository;

    //  Values which are lesser than 0 mean absence of timer
    @Value("${leaderboard.max.durationForInactiveState:86400000}")
    private Long maxDurationForInactiveState;

    private final LeaderboardToResponseReadMapper leaderboardToResponseReadMapper;

    public LeaderboardServiceImpl(StringRedisTemplate stringRedisTemplate, LeaderboardInfoRepository LeaderboardInfoRepository,
                                  RedisScript<String> mutableLeaderboardScript, RedisScript<String> immutableLeaderboardScript,
                                  MapperCreateLeaderboardInfo mapperCreateLeaderboardInfo, RedisScript<String> createLeaderboardScript,
                                  CachedUserRepository cachedUserRepository, RedisScript<String> createUserCachedScript,
                                  RedisScript<String> deleteLeaderboardScript,
                                  RedisScript<String> confirmLbCreationScript,
                                  SagaControllingStateRepository sagaControllingStateRepository, LeaderboardToResponseReadMapper leaderboardToResponseReadMapper) {

        this.stringRedisTemplate = stringRedisTemplate;
        this.leaderboardInfoRepository = LeaderboardInfoRepository;
        this.mutableLeaderboardScript = mutableLeaderboardScript;
        this.immutableLeaderboardScript = immutableLeaderboardScript;
        this.mapperCreateLeaderboardInfo = mapperCreateLeaderboardInfo;
        this.createLeaderboardScript = createLeaderboardScript;
        this.cachedUserRepository = cachedUserRepository;
        this.createUserCachedScript = createUserCachedScript;
        this.deleteLeaderboardScript = deleteLeaderboardScript;
        this.confirmLbCreationScript = confirmLbCreationScript;
        this.sagaControllingStateRepository = sagaControllingStateRepository;
        this.leaderboardToResponseReadMapper = leaderboardToResponseReadMapper;
    }

    @Override
    public void increaseUserScore(UserScoreEvent changeEvent) throws CannotCreateCachedUserException {

        userCachedCheck(changeEvent.getUserId());
        executeScoreChange(
                changeEvent.getLbId(),
                changeEvent.getUserId(),
                changeEvent.getScore(),
                true
        );
        log.info("incremented score for user: {}", changeEvent.getUserId());
    }

    private void userCachedCheck(Long userId) throws CannotCreateCachedUserException {
        if (cachedUserRepository.existsById(userId)) {
            return;
        }

        List<String> keys = List.of("user_cached:" + userId,
                "user_cached:" + userId + ":daily_attempts",
                "user_cached:" + userId + ":total_attempts"
        );

        String execute = stringRedisTemplate.execute(
                createUserCachedScript,
                keys,
                userId.toString()
        );

        if(!execute.equals("OK")) {
            log.error("Adding new user failed with id {}", userId);
            throw new CannotCreateCachedUserException("Cannot create user cached version");
        }
        log.info("Adding new user ended successfully for id {}", userId);
    }

    @SneakyThrows
    @Override
    @Transactional
    public void addNewScore(UserScoreUploadEvent uploadEvent) {
        userCachedCheck(uploadEvent.getUserId());
        executeScoreChange(

                uploadEvent.getLbId(),
                uploadEvent.getUserId(),
                uploadEvent.getScore(),
                false
        );
        log.info("added score for user: {}", uploadEvent.getUserId());
    }

    private void executeScoreChange(String lbId, Long userId, double scoreDelta, boolean isMutable) {
        LeaderboardInfo info = leaderboardInfoRepository.findById(lbId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Leaderboard with id " + lbId + " does not exist")
                );


        List<String> keys = getStrings(userId, lbId, isMutable);
        String execute = stringRedisTemplate.execute(
                isMutable? mutableLeaderboardScript : immutableLeaderboardScript,
                keys,
                userId.toString(),
                String.valueOf(scoreDelta),
                String.valueOf(info.getMaxEventsPerUser()),
                String.valueOf(info.getMaxEventsPerUserPerDay()),
                info.getRegions().toString(),
                String.valueOf(info.getGlobalRange()),
                lbId
        );
        if (!"success".equals(execute)) {
            throw new IllegalStateException("Failed to update score for user " + userId + ": " + execute);
        }
    }

    private static List<String> getStrings(Long userId, String lbId, boolean isMutable) {
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

    @SneakyThrows
    @Override
    @Transactional
    public String createLeaderboard(InitLeaderboardCreateEvent request, String region) {
        if(request.getMaxScore() < -1 && request.getMaxScore() == request.getInitialValue()) {
            throw new IllegalArgumentException("initial value cannot be equal or greater than max score");
        }
        userCachedCheck(request.getOwnerId());
        LeaderboardInfo map = mapperCreateLeaderboardInfo.map(request);


        String id = map.getKey();

        List<String> keys = List.of(
                id,
                map.getInfoKey(),
                map.getSignalKey()
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
                maxDurationForInactiveState
        );
        return map.getId();
    }

    @Override
    @SneakyThrows
    @Transactional
    public void initUserScore(InitUserScoreRequest request, long userId) {

        LeaderboardInfo lb = leaderboardInfoRepository.findById(request.getLeaderboardId())
                .orElseThrow();
        userCachedCheck(userId);

        if(!lb.isPublic()){
            throw new IllegalArgumentException("cannot join to private leaderboard");
        }

        stringRedisTemplate.opsForZSet().add(lb.getKey(),
                String.valueOf(userId),
                lb.getInitialValue()
        );

    }



    @Override
    @Transactional
    public void deleteLeaderboard(String lbId, String sagaId) {

        LeaderboardInfo byId =
                leaderboardInfoRepository.findById(lbId).orElseThrow();

        SagaControllingState sagaControllingState = sagaControllingStateRepository
                .findById(sagaId).orElseThrow();

        stringRedisTemplate.execute(deleteLeaderboardScript,
                List.of(
                        byId.getKey(),
                        byId.getInfoKey(),
                        byId.getSignalKey(),
                        sagaControllingState.getKey()
                        )
        );
    }

    @Override
    public void confirmLbCreation(String lbId) {
        LeaderboardInfo byId =
                leaderboardInfoRepository.findById(lbId).orElseThrow();

        long milli = Duration.between(
                Instant.now(),
                byId.getExpireAt().toInstant(ZoneOffset.UTC)
        ).toMillis();

        List<String> keys = List.of(
                byId.getInfoKey(),
                byId.getSignalKey());

        String execute = stringRedisTemplate.execute(confirmLbCreationScript,
                keys,
                milli + ""
        );

        if(!"OK".equals(execute)) {
            log.error("Cannot create leaderboard");
            throw new IllegalStateException();
        }
    }

    @Override
    public LeaderboardResponse getLeaderboard(String id) {

        return leaderboardInfoRepository
                .findById(id)
                .map(leaderboardToResponseReadMapper::map)
                .orElseThrow();

    }

    @Override
    public boolean userExistsById(Long id, String lbId) {

        Optional<Long> rank = Optional.ofNullable(
                stringRedisTemplate.opsForZSet()
                        .rank(lbId, id.toString())
        );

        return rank.isPresent();

    }

}