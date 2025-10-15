package com.jzargo.leaderboardmicroservice;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.handler.RedisLocalLeaderboardHandler;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.messaging.GlobalLeaderboardEvent;
import com.jzargo.messaging.UserLocalUpdateEvent;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.jzargo.leaderboardmicroservice.IntegrationTestHelper.*;
import static com.jzargo.leaderboardmicroservice.handler.RedisGlobalLeaderboardUpdateHandler.GLOBAL_STREAM_KEY;
import static com.jzargo.leaderboardmicroservice.handler.RedisLocalLeaderboardHandler.LOCAL_STREAM_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.kafka.test.utils.ContainerTestUtils.waitForAssignment;


// Test class to verify integration between Redis Streams and Kafka


@DirtiesContext
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfigHelper.class)
@EmbeddedKafka(partitions = 3, topics = {KafkaConfig.LEADERBOARD_UPDATE_TOPIC})
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

    @SpyBean
    private RedisLocalLeaderboardHandler  redisLocalLeaderboardHandler;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;


    BlockingQueue<GlobalLeaderboardEvent> globalRecordsQueue;
    BlockingQueue<UserLocalUpdateEvent> localRecordsQueue;


    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
    private String trustedPackages;

    @Value("${kafka.consumer.group-id}")
    private String groupId;

    @BeforeEach
    public void setUp() {
        DefaultKafkaConsumerFactory<String,Object> factory = new DefaultKafkaConsumerFactory<>(getProperties());

        ContainerProperties globalProps = new ContainerProperties(KafkaConfig.LEADERBOARD_UPDATE_TOPIC);
        ContainerProperties localProps = new ContainerProperties(KafkaConfig.LEADERBOARD_UPDATE_TOPIC);

        KafkaMessageListenerContainer<String, Object> globalContainer =
                new KafkaMessageListenerContainer<>(factory, globalProps);
        KafkaMessageListenerContainer<String, Object> localContainer =
                new KafkaMessageListenerContainer<>(factory, localProps);

        globalContainer.setupMessageListener((MessageListener<String, GlobalLeaderboardEvent>) globalRecordsQueue::add);
        localContainer.setupMessageListener((MessageListener<String, UserLocalUpdateEvent>) localRecordsQueue::add);


        globalContainer.start();
        localContainer.start();

        waitForAssignment(globalContainer, embeddedKafkaBroker.getPartitionsPerTopic());
        waitForAssignment(localContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    private Map<String, Object> getProperties() {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES, trustedPackages,
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );
    }


    @PostConstruct
    public void prepareLeaderboards(){
        integrationTestHelper.createLeaderboard(true);
        integrationTestHelper.createLeaderboard(false);
        integrationTestHelper.createUser();
    }


    @Test
    public void testHandleAndPostGlobalMessageToKafkaTopic_whenGlobalLocalStreamsHappened() throws InterruptedException {
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

        ArgumentCaptor<MapRecord<String, Object, Object>> recordCaptor =
                ArgumentCaptor.forClass((Class) MapRecord.class);

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

        assertGlobalRange(leaderboardInfo);

        verify(redisLocalLeaderboardHandler, timeout(3000))
                .handleMessage(recordCaptor.capture());

        MapRecord<String, Object, Object> captured = recordCaptor.getValue();
        Long oldRank = (Long) captured.getValue().get("oldRank");
        assertLocalRange(leaderboardInfo, oldRank);

    }

    private void assertLocalRange(LeaderboardInfo leaderboardInfo, Long oldRank) throws InterruptedException {
        UserLocalUpdateEvent poll = localRecordsQueue.poll(3, TimeUnit.SECONDS);

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

    private void assertGlobalRange(LeaderboardInfo leaderboardInfo) throws InterruptedException {
        GlobalLeaderboardEvent poll = globalRecordsQueue.poll(3, TimeUnit.SECONDS);

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
