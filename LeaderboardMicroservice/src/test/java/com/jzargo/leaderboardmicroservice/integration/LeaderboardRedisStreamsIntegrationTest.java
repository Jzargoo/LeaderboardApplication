package com.jzargo.leaderboardmicroservice.integration;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.messaging.GlobalLeaderboardEvent;
import com.jzargo.messaging.UserLocalUpdateEvent;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
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
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.jzargo.leaderboardmicroservice.integration.IntegrationTestHelper.*;
import static com.jzargo.leaderboardmicroservice.config.RedisConfig.*;
import static org.junit.jupiter.api.Assertions.*;


// Test class to verify integration between Redis Streams and Kafka


@DirtiesContext
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfigHelper.class)
@EmbeddedKafka(partitions = 3,brokerProperties = "num.brokers=3", topics = {KafkaConfig.LEADERBOARD_UPDATE_TOPIC})
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
    private TestConsumer testConsumer;

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


        MapRecord<String, Object, Object> captured = Objects.requireNonNull(stringRedisTemplate.opsForStream()
                .read(
                        Consumer.from(LOCAL_GROUP_NAME, "consumer-1"),
                        StreamOffset.create(LOCAL_STREAM_KEY, ReadOffset.lastConsumed())
                )).getFirst();

        long oldRank = Long.parseLong( (String) captured.getValue().get("oldRank"));
        assertLocalRange(oldRank);
        assertGlobalRange(leaderboardInfo);

    }

    private void assertLocalRange(Long oldRank){
        UserLocalUpdateEvent poll = Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> testConsumer.getLocalRecordsQueue().poll(), Objects::nonNull);

        assertNotNull(poll);
        assertNotNull(poll.getEntries());

        ArrayList<GlobalLeaderboardEvent.Entry> top = new ArrayList<>();

        Long newRank = stringRedisTemplate.opsForZSet()
            .reverseRank(MUTABLE_BOARD, USER_ID.toString());
        Objects.requireNonNull(
                stringRedisTemplate.opsForZSet()
                        .reverseRangeWithScores(MUTABLE_BOARD, oldRank, newRank))
                .forEach(tuple -> {
                    if(tuple.getValue() == null || tuple.getScore() == null) return;

                    GlobalLeaderboardEvent.Entry entry = new GlobalLeaderboardEvent.Entry();

                    entry.setUserId(Long.parseLong(tuple.getValue()));
                    entry.setScore(tuple.getScore());
                    top.add(entry);
                });

        assertArrayEquals(top.toArray(), poll.getEntries().toArray());
    }

    private void assertGlobalRange(LeaderboardInfo leaderboardInfo){
         GlobalLeaderboardEvent poll = Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> testConsumer.getGlobalRecordsQueue().poll(), Objects::nonNull);

        assertNotNull(poll);
        assertNotNull(poll.getTopNLeaderboard());

        ArrayList<GlobalLeaderboardEvent.Entry> top = new ArrayList<>();

        Objects.requireNonNull(
                stringRedisTemplate.opsForZSet()
                        .reverseRangeWithScores(MUTABLE_BOARD, 0, leaderboardInfo.getGlobalRange() - 1))
                .forEach(tuple -> {
                    if(tuple.getValue() == null || tuple.getScore() == null) return;
                    GlobalLeaderboardEvent.Entry entry = new GlobalLeaderboardEvent.Entry();
                    entry.setUserId(Long.parseLong(tuple.getValue()));
                    entry.setScore(tuple.getScore());
                    top.add(entry);
                });

        assertArrayEquals(top.toArray(), poll.getTopNLeaderboard().toArray());
    }


    @AfterAll
    public static void tearDown(){
        if(redisContainer != null){
            redisContainer.stop();
        }
    }
}
