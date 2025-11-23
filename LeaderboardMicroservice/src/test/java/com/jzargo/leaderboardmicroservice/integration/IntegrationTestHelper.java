package com.jzargo.leaderboardmicroservice.integration;

import com.jzargo.region.Regions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.List;

public class IntegrationTestHelper {
    public static final  Long MUTABLE_LEADERBOARD_ID= 10234L;
    public static final  Long IMMUTABLE_LEADERBOARD_ID= 10235L;
    public static final  String IMMUTABLE_BOARD = "leaderboard:" + IMMUTABLE_LEADERBOARD_ID + ":immutable";
    public static final  String MUTABLE_BOARD = "leaderboard:" + MUTABLE_LEADERBOARD_ID + ":mutable";
    public static final  Long USER_ID = 10L;
    public static final  Double INIT = 0.0;
    public static final  String CODE = Regions.GLOBAL.getCode();
    public static final  String USERNAME = "Alex111";
    public static final  String DESCRIPTION = "Top players 2025";
    public static final Long OWNER_ID = 999L;
    public static final Double MAX_SCORE = 100000.0;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisScript<String> createLeaderboardScript;
    @Autowired
    private RedisScript<String> createUserCachedScript;

    public IntegrationTestHelper(StringRedisTemplate stringRedisTemplate, RedisScript<String> createLeaderboardScript) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.createLeaderboardScript = createLeaderboardScript;
    }

    public void initializeUser() {
        stringRedisTemplate.opsForZSet().add(
                MUTABLE_BOARD,
                USER_ID.toString(),
                INIT
        );

        stringRedisTemplate.opsForZSet().add(
                IMMUTABLE_BOARD,
                USER_ID.toString(),
                INIT
        );
    }

    public void createUser() {
        stringRedisTemplate.execute(createUserCachedScript,
                List.of(
                        "user_cached:" + USER_ID,
                        "user_cached:" + USER_ID + ":daily_attempts",
                        "user_cached:" + USER_ID + ":total_attempts"
                        ),
                USER_ID.toString(),
                USERNAME,
                Regions.GLOBAL.getCode()
        );
        initializeUser();
    }
    public void createLeaderboard(boolean b) {
        Long lbId = b ?
                MUTABLE_LEADERBOARD_ID:
                IMMUTABLE_LEADERBOARD_ID;
        List<String> keys = Arrays.asList(
                b?
                        MUTABLE_BOARD:
                        IMMUTABLE_BOARD
                ,
                "leaderboard_information:" + lbId

        );
        stringRedisTemplate.execute(
                createLeaderboardScript,
                keys,
                OWNER_ID.toString(),
                INIT.toString(),
                lbId.toString(),
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
    }
}
