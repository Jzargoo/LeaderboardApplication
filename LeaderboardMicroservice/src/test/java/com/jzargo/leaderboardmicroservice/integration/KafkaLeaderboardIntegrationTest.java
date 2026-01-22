package com.jzargo.leaderboardmicroservice.integration;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.exceptions.CannotCreateCachedUserException;
import com.jzargo.leaderboardmicroservice.handler.KafkaUserScoreHandler;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.leaderboardmicroservice.service.LeaderboardServiceImpl;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EmbeddedKafka
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@SpringBootTest
public class KafkaLeaderboardIntegrationTest {

    private final double NEW_SCORE = 10.0;
    private final String LEADERBOARD_ID = "10241";
    private final long USER_ID = 2L;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private StringRedisTemplate stringRedisTemplate;
    @MockitoBean
    private SagaControllingStateRepository sagaControllingStateRepository;
    @MockitoBean
    private LeaderboardInfoRepository leaderboardInfoRepository;
    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean
    private LeaderboardServiceImpl leaderboardService;

    @Autowired
    private KafkaPropertyStorage kafkaPropertyStorage;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoSpyBean
    private KafkaUserScoreHandler kafkaUserScoreHandler;

    @Test
    public void receiveMutableKafkaMessage(){
        when(stringRedisTemplate.opsForValue()
                .setIfAbsent(
                        anyString(),
                        anyString(),
                        any(Duration.class)
                )
        ).thenReturn(true);
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

        String messageId = KafkaUtils.newMessageId();

        KafkaUtils.addCommonHeaders(
                record,
                LEADERBOARD_ID,
                kafkaPropertyStorage.getHeaders().getMessageId(),
                messageId
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

        assertEquals(messageId, messId.getValue());

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

        String messageId = KafkaUtils.newMessageId();

        ProducerRecord<String, Object> record = KafkaUtils.createRecord(
                kafkaPropertyStorage.getTopic().getNames()
                        .getLeaderboardEvent(),
                String.valueOf(USER_ID),
                userScoreUploadEvent);

        KafkaUtils.addCommonHeaders(
                record,
                LEADERBOARD_ID,
                kafkaPropertyStorage.getHeaders().getMessageId(),
                messageId
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