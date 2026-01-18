package com.jzargo.leaderboardmicroservice.integration;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static com.jzargo.leaderboardmicroservice.config.RedisConfig.GLOBAL_STREAM_KEY;
import static com.jzargo.leaderboardmicroservice.config.RedisConfig.LOCAL_STREAM_KEY;
import static com.jzargo.leaderboardmicroservice.integration.IntegrationTestHelper.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;


// Test class to verify integration between invoking stream and sending in kafka


@DirtiesContext
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfigHelper.class)
@SpringBootTest
class LeaderboardRedisStreamsIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(
            "redis:latest"
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }


    @Autowired
    private RedisScript<String> mutableLeaderboardScript;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

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
    public void testHandleAndPostGlobalMessageToKafkaTopic_whenGlobalLocalStreamsHappened() {
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


        String execute = stringRedisTemplate.execute(
                mutableLeaderboardScript, keys,
                USER_ID.toString(),
                "50",
                String.valueOf(leaderboardInfo.getMaxEventsPerUser()),
                String.valueOf(leaderboardInfo.getMaxEventsPerUserPerDay()),
                String.join(",", leaderboardInfo.getRegions()),
                String.valueOf(leaderboardInfo.getGlobalRange()),
                MUTABLE_LEADERBOARD_ID.toString()
        );

        assertArrayEquals("success".toCharArray(), execute.toCharArray());

        Long sizeGlobal = stringRedisTemplate.opsForStream().size(GLOBAL_STREAM_KEY);
        Long sizeLocal = stringRedisTemplate.opsForStream().size(LOCAL_STREAM_KEY);



        assertEquals(1L, sizeLocal, "Local stream should have one message");
        assertEquals(1L, sizeGlobal, "Global stream should have one message");

        verify(kafkaTemplate, timeout(5000).times(2)).send(any(ProducerRecord.class));
    }



    @AfterAll
    public static void tearDown(){
        if(redisContainer != null){
            redisContainer.stop();
        }
    }
}
