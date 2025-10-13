package com.jzargo.leaderboardmicroservice;

import com.jzargo.region.Regions;
import jakarta.annotation.PostConstruct;
import org.junit.AfterClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

// Test class to verify integration between Redis Streams and Kafka
@Testcontainers
@EmbeddedKafka
@ActiveProfiles("test")
@SpringBootTest(properties = "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class LeaderboardRedisStreamsIntegrationTest {

    private static final String LEADERBOARD_KEY_MUTABLE = "leaderboard:JH5yTgF$2hF1!#:mutable";
    private static final String LEADERBOARD_ID_MUTABLE = "JH5yTgF$2hF1!#";
    private static final String LEADERBOARD_ID_IMMUTABLE = "BqByr#!l";
    private static final String LEADERBOARD_KEY_IMMUTABLE = "leaderboard:BqByr#!l:immutable";
    private static final Long USER_ID = 74L;
    private static final Double INIT = 0.0;
    private static final String CODE = Regions.GLOBAL.getCode();
    private static final String USERNAME = "Alex111";
    private static final String DESCRIPTION = "Top players 2025";
    private static final Long OWNER_ID = 999L;
    private static final Double MAX_SCORE = 100000.0;
    @Container
    private static GenericContainer<?> redisContainer = new GenericContainer<>(
            "redis:latest"
    ).withExposedPorts(6379);

    @Autowired
    private RedisScript<String> mutableLeaderboardScript;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisScript<String> createLeaderboardScript;

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
                LEADERBOARD_KEY_IMMUTABLE,
                USER_ID.toString(),
                INIT
        );
    }

    private void createLeaderboard(boolean b) {
        String lbId = b ?
                LEADERBOARD_ID_MUTABLE :
                LEADERBOARD_ID_IMMUTABLE + 1;
        List<String> keys = Arrays.asList(
                b?
                        LEADERBOARD_KEY_MUTABLE :
                        LEADERBOARD_KEY_IMMUTABLE
                ,
                "leaderboard_information:" + lbId

        );
        stringRedisTemplate.execute(
                createLeaderboardScript,
                keys,
                OWNER_ID.toString(),
                INIT.toString(),
                lbId,
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

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getFirstMappedPort());
    }

    @Test
    public void testHandleAndPostGlobalMessageToKafkaTopic() {
        stringRedisTemplate.execute(mutableLeaderboardScript,

        )
    }

    @AfterClass
    public static void tearDown(){
        if(redisContainer != null){
            redisContainer.stop();
        }
    }
}
