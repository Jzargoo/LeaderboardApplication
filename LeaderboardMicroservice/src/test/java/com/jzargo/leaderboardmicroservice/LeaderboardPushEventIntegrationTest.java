package com.jzargo.leaderboardmicroservice;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.exceptions.CannotCreateCachedUserException;
import com.jzargo.leaderboardmicroservice.handler.RedisGlobalLeaderboardUpdateHandler;
import com.jzargo.leaderboardmicroservice.handler.RedisLocalLeaderboardHandler;
import com.jzargo.leaderboardmicroservice.repository.CachedUserRepository;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static com.jzargo.leaderboardmicroservice.IntegrationTestHelper.*;

@Testcontainers
@EnableAutoConfiguration(exclude = { KafkaAutoConfiguration.class })
@SpringBootTest
@DirtiesContext
@Import(TestConfigHelper.class)
@ActiveProfiles("test")
class LeaderboardPushEventIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:latest")
    ).withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private LeaderboardService leaderboardService;
    @Autowired
    private LeaderboardInfoRepository leaderboardInfoRepository;
    @Autowired
    private CachedUserRepository cachedUserRepository;

    @MockitoBean
    private RedisGlobalLeaderboardUpdateHandler redisGlobalLeaderboardUpdateHandler;
    @MockitoBean
    private RedisLocalLeaderboardHandler redisLocalLeaderboardHandler;
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoBean
    private KafkaConfig kafkaConfig;
    @Autowired
    private IntegrationTestHelper integrationTestHelper;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getFirstMappedPort().toString());
    }

    @PostConstruct
    public void prepareLeaderboards(){
        integrationTestHelper.createLeaderboard(true);
        integrationTestHelper.createLeaderboard(false);
        integrationTestHelper.initializeUser();
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

        UserScoreUploadEvent userScoreUploadEvent = new UserScoreUploadEvent();
        userScoreUploadEvent.setUserId(USER_ID);
        userScoreUploadEvent.setUsername(USERNAME);
        userScoreUploadEvent.setRegion(CODE);
        userScoreUploadEvent.setLbId(IMMUTABLE_LEADERBOARD_ID.toString());

        leaderboardService.addNewScore(userScoreUploadEvent);



        assertLeaderboardState(
                IMMUTABLE_LEADERBOARD_ID,
                IMMUTABLE_BOARD,
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
        userScoreEvent.setLbId(MUTABLE_LEADERBOARD_ID.toString());

        try {
            leaderboardService.increaseUserScore(userScoreEvent);
        } catch (CannotCreateCachedUserException e) {
            throw new RuntimeException(e);
        }

        assertLeaderboardState(
                MUTABLE_LEADERBOARD_ID,
                MUTABLE_BOARD,
                true,
                userScoreEvent.getScore() + INIT
        );
    }

    @AfterAll
    static void stopContainer(){
        redisContainer.stop();
    }
}
