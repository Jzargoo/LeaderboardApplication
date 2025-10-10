package com.jzargo.leaderboardmicroservice;

import com.jzargo.leaderboardmicroservice.repository.CachedUserRepository;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import com.jzargo.region.Regions;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LeaderboardPushEventIntegrationTest {

    private static final  Long LEADERBOARD_ID= 10234L;
    private static final  String LEADERBOARD_KEY_IMMUTABLE = "leaderboard:" + (LEADERBOARD_ID + 1) + ":immutable";
    private static final  String LEADERBOARD_KEY_MUTABLE = "leaderboard:" + LEADERBOARD_ID + ":mutable";
    private static final  Long USER_ID = 10L;
    private static final  Double INIT = 0.0;
    private static final  String CODE = Regions.GLOBAL.getCode();
    private static final  String USERNAME = "Alex111";
    private static final  String DESCRIPTION = "Top players 2025";
    private static final Long OWNER_ID = 999L;
    private static final Double MAX_SCORE = 100000.0;

    static GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:latest")
    ).withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisScript<String> createLeaderboardScript;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private LeaderboardInfoRepository leaderboardInfoRepository;
    @Autowired
    private CachedUserRepository cachedUserRepository;

    @BeforeAll
    static void prepareRedis(){
        redisContainer.start();
        System.setProperty("spring.data.redis.host", redisContainer.getHost());
        System.setProperty("spring.data.redis.port",
                redisContainer.getFirstMappedPort().toString());

    }

    @PostConstruct
    public void prepareLeaderboards(){
        createLeaderboard(true);
        createLeaderboard(false);

        initializeUser();
    }

    private void initializeUser() {
        stringRedisTemplate.opsForZSet().add(
                LEADERBOARD_KEY_MUTABLE,
                USER_ID.toString(),
                INIT
                );

        stringRedisTemplate.opsForZSet().add(
                "leaderboard:" + LEADERBOARD_ID + ":immutable",
                USER_ID.toString(),
                INIT
        );
    }

    private void createLeaderboard(boolean b) {
        List<String> keys = Arrays.asList(
                b?
                        LEADERBOARD_KEY_MUTABLE :
                        LEADERBOARD_KEY_IMMUTABLE
                ,
                "leaderboard_information:" + LEADERBOARD_ID
        );
        List<String> args = Arrays.asList(
                OWNER_ID.toString(),
                INIT.toString(),
                LEADERBOARD_ID.toString(),
                DESCRIPTION,
                "true",
                b + "",
                "false",
                "100",
                "2025-10-10T18:00",
                "2025-12-31T23:59",
                MAX_SCORE.toString(),
                CODE,
                "10",
                "3"
        );
        stringRedisTemplate.execute(
                createLeaderboardScript,
                keys, args
        );

    }

    private void assertLeaderboardState(
            Long leaderboardId,
            String leaderboardKey,
            boolean expectedMutable,
            double expectedScore
    ) {
        var leaderboardInfo = leaderboardInfoRepository.findById(leaderboardId.toString()).orElseThrow();

        assertNotNull(leaderboardInfo, "leaderboard info did not find");
        assertEquals(DESCRIPTION, leaderboardInfo.getDescription());
        assertEquals(expectedMutable, leaderboardInfo.isMutable());
        assertEquals(OWNER_ID, leaderboardInfo.getOwnerId());
        assertEquals(MAX_SCORE, leaderboardInfo.getMaxScore());

        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().rangeWithScores(leaderboardKey, 0, -1);

        assertNotNull(typedTuples, "cannot find zset");

        ZSetOperations.TypedTuple<String> found = typedTuples.stream()
                .filter(tuple -> Objects.equals(USER_ID.toString(), tuple.getValue()))
                .findFirst()
                .orElse(null);

        assertNotNull(found, "User could not be found in leaderboard");
        assertEquals(expectedScore, found.getScore(), 0.001, "Actual score and expected mismatch");

        boolean cachedExists = cachedUserRepository.existsById(USER_ID);
        assertTrue(cachedExists, "Unexpected cached user existence");
    }

    @Test
    public void handleSuccessfulUserImmutableChangeEvent() {
        Long immutableId = LEADERBOARD_ID + 1;

        UserScoreUploadEvent userScoreUploadEvent = new UserScoreUploadEvent();
        userScoreUploadEvent.setUserId(USER_ID);
        userScoreUploadEvent.setUsername(USERNAME);
        userScoreUploadEvent.setRegion(CODE);
        userScoreUploadEvent.setLbId(immutableId.toString());

        leaderboardService.addNewScore(userScoreUploadEvent);

        assertLeaderboardState(
                immutableId,
                LEADERBOARD_KEY_IMMUTABLE,
                false,
                userScoreUploadEvent.getScore()
        );
    }

    @Test
    public void handleSuccessfulUserMutableChangeEvent() {
        UserScoreEvent userScoreEvent = new UserScoreEvent();
        userScoreEvent.setUserId(USER_ID);
        userScoreEvent.setScore(10.0);
        userScoreEvent.setRegion(CODE);
        userScoreEvent.setUsername(USERNAME);
        userScoreEvent.setLbId(LEADERBOARD_ID.toString());

        leaderboardService.increaseUserScore(userScoreEvent);

        assertLeaderboardState(
                LEADERBOARD_ID,
                LEADERBOARD_KEY_MUTABLE,
                true,
                userScoreEvent.getScore() + INIT
        );
    }

    @AfterAll
    static void stopContainer(){
        redisContainer.stop();
    }
}
