package com.jzargo.leaderboardmicroservice.integration;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.exceptions.CannotCreateCachedUserException;
import com.jzargo.leaderboardmicroservice.handler.KafkaUserScoreHandler;
import com.jzargo.leaderboardmicroservice.handler.RedisGlobalLeaderboardUpdateHandler;
import com.jzargo.leaderboardmicroservice.handler.RedisLocalLeaderboardHandler;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EmbeddedKafka
@DirtiesContext
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class)
@SpringBootTest(properties = "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class KafkaLeaderboardIntegrationTest {
    private final double NEW_SCORE = 10.0;
    private final String LEADERBOARD_ID = "10241";
    private final long USER_ID = 2L;

    @MockitoBean
    private LeaderboardService leaderboardService;
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean
    private RedisGlobalLeaderboardUpdateHandler redisGlobalLeaderboardUpdateHandler;
    @MockitoBean
    private RedisLocalLeaderboardHandler redisLocalLeaderboardHandler;

    @Autowired
    private KafkaPropertyStorage kafkaPropertyStorage;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoSpyBean
    private KafkaUserScoreHandler kafkaUserScoreHandler;

    @BeforeEach
    void mockingBeans(){
        MockitoAnnotations.openMocks(this);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForValue().setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)
        )).thenReturn(true);
    }

    @Test
    public void receiveMutableKafkaMessage(){
        UserScoreEvent userScoreEvent = new UserScoreEvent(
                NEW_SCORE,
                USER_ID,
                LEADERBOARD_ID,
                new HashMap<>()
        );
        try {
            doNothing()
                    .when(leaderboardService)
                    .increaseUserScore(
                        any(UserScoreEvent.class)
                    );
        } catch (CannotCreateCachedUserException e) {
            throw new RuntimeException(e);
        }

        ProducerRecord<String, Object> record = KafkaUtils.createRecord(
                kafkaPropertyStorage.getTopic().getNames().getLeaderboardEvent(),
                String.valueOf(USER_ID),
                userScoreEvent
        );
        KafkaUtils.addCommonHeaders(
                record,
                LEADERBOARD_ID,
                kafkaPropertyStorage.getHeaders().getMessageId()
                );

        kafkaTemplate.send(record);


        ArgumentCaptor<UserScoreEvent> event = ArgumentCaptor.forClass(UserScoreEvent.class);
        ArgumentCaptor<String> messId = ArgumentCaptor.forClass(String.class);
        verify(
                kafkaUserScoreHandler,
                timeout(
                        Duration.ofSeconds(5).toMillis())
                        .times(1)
        ).handleUserMutableChangeEvent(
                event.capture(), messId.capture()
        );

        assertEquals(
                kafkaPropertyStorage.getHeaders()
                        .getMessageId(), messId.getValue());

        assertEquals(userScoreEvent.getScore(), event.getValue().getScore());
        assertEquals(userScoreEvent.getUserId(), event.getValue().getUserId());
        assertEquals(userScoreEvent.getLbId(), event.getValue().getLbId());
    }


    @Test
    public void receiveImmutableKafkaMessage(){
        UserScoreUploadEvent userScoreUploadEvent = new UserScoreUploadEvent(
                LEADERBOARD_ID,
                USER_ID,
                NEW_SCORE,
                new HashMap<>()
        );
        doNothing()
                .when(leaderboardService)
                .addNewScore(
                        any(UserScoreUploadEvent.class)
                );

        String messageId = UUID.randomUUID().toString();

        ProducerRecord<String, Object> record = KafkaUtils.createRecord(
                kafkaPropertyStorage.getTopic().getNames()
                        .getLeaderboardEvent(),
                String.valueOf(USER_ID),
                userScoreUploadEvent);

        KafkaUtils.addCommonHeaders(
                record,
                LEADERBOARD_ID,
                kafkaPropertyStorage.getHeaders().getMessageId()
                );

        kafkaTemplate.send(record);

        ArgumentCaptor<UserScoreUploadEvent> event = ArgumentCaptor.forClass(UserScoreUploadEvent.class);
        ArgumentCaptor<String> messId = ArgumentCaptor.forClass(String.class);

        verify(
                kafkaUserScoreHandler,
                timeout(
                        Duration.ofSeconds(5).toMillis())
                        .times(1)
        ).handleUserImmutableChangeEvent(
                event.capture(), messId.capture()
        );

        assertEquals(messageId, messId.getValue());
        assertEquals(userScoreUploadEvent.getScore(), event.getValue().getScore());
        assertEquals(userScoreUploadEvent.getUserId(), event.getValue().getUserId());
        assertEquals(userScoreUploadEvent.getLbId(), event.getValue().getLbId());

    }

}
