package com.jzargo.leaderboardmicroservice;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.jzargo.leaderboardmicroservice.IntegrationTestHelper.*;
import static com.jzargo.leaderboardmicroservice.handler.RedisGlobalLeaderboardUpdateHandler.GLOBAL_STREAM_KEY;
import static com.jzargo.leaderboardmicroservice.handler.RedisLocalLeaderboardHandler.LOCAL_STREAM_KEY;

// Test class to verify integration between Redis Streams and Kafka
@EmbeddedKafka
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext
@Import(TestConfigHelper.class)
@SpringBootTest(properties = "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}")
class LeaderboardRedisStreamsIntegrationTest {
    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(
            "redis:latest"
    ).withExposedPorts(6379);

    static {
        redisContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        System.out.println(">>> Redis test container host=" + redisContainer.getHost());
        System.out.println(">>> Redis test container port=" + redisContainer.getFirstMappedPort());
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getFirstMappedPort().toString());
    }

    @Autowired
    private RedisScript<String> mutableLeaderboardScript;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IntegrationTestHelper integrationTestHelper;
    @Autowired
    private LeaderboardInfoRepository leaderboardInfoRepository;

    @PostConstruct
    public void prepareLeaderboards(){
        integrationTestHelper.createLeaderboard(true);
        integrationTestHelper.createLeaderboard(false);
        integrationTestHelper.createUser();
    }




    @Test
    public void testHandleAndPostGlobalMessageToKafkaTopic() {
        List<String> keys = List.of(
                "user_cached:" + USER_ID + ":daily_attempts",
                "user_cached:" + USER_ID + ":total_attempts",
                MUTABLE_BOARD,
                "user_cached:" + USER_ID,
                GLOBAL_STREAM_KEY,
                LOCAL_STREAM_KEY
        );
        LeaderboardInfo leaderboardInfo = leaderboardInfoRepository.findById(MUTABLE_LEADERBOARD_ID.toString())
                .orElseThrow();

        stringRedisTemplate.execute(
                mutableLeaderboardScript, keys,
                USER_ID.toString(),
                "50",
                String.valueOf(leaderboardInfo.getMaxEventsPerUser()),
                String.valueOf(leaderboardInfo.getMaxEventsPerUserPerDay()),
                leaderboardInfo.getRegions().toString(),
                String.valueOf(leaderboardInfo.getGlobalRange()),
                MUTABLE_LEADERBOARD_ID.toString()
        );


    }

    @AfterAll
    public static void tearDown(){
        if(redisContainer != null){
            redisContainer.stop();
        }
    }
}
