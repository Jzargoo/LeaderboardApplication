package com.jzargo.leaderboardmicroservice.unit;


import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.handler.ExpirationLeaderboardPubSubHandler;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.*;
import static org.mockito.Mockito.*;

public class ExpirationLeaderboardPubSubHandlerTest {

    @Mock
    private LeaderboardInfoRepository leaderboardInfoRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SagaLeaderboardCreate sagaLeaderboardCreate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private ExpirationLeaderboardPubSubHandler handler;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void testOnMessage_BlankKey() {
        Message message = mock(Message.class);
        when(message.toString()).thenReturn("");

        handler.onMessage(message, null);

        verifyNoInteractions(leaderboardInfoRepository, kafkaTemplate, sagaLeaderboardCreate, zSetOperations);
    }

    @Test
    void testOnMessage_InactiveLeaderboard() {
        String expiredKey = "leaderboard_signal:123";
        Message message = mock(Message.class);
        when(message.toString()).thenReturn(expiredKey);

        LeaderboardInfo lb = mock(LeaderboardInfo.class);
        when(lb.isActive()).thenReturn(false);
        when(lb.getId()).thenReturn("123");
        when(leaderboardInfoRepository.findById("123")).thenReturn(Optional.of(lb));
        when(sagaLeaderboardCreate.stepOutOfTime("123")).thenReturn(true);

        handler.onMessage(message, null);

        verify(sagaLeaderboardCreate).stepOutOfTime("123");
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testOnMessage_ActiveLeaderboardWithData() {
        String expiredKey = "leaderboard_signal:123";
        Message message = mock(Message.class);
        when(message.toString()).thenReturn(expiredKey);

        LeaderboardInfo lb = mock(LeaderboardInfo.class);
        when(lb.isActive()).thenReturn(true);
        when(lb.getId()).thenReturn("123");
        when(lb.getName()).thenReturn("LB Name");
        when(lb.getDescription()).thenReturn("LB Description");
        when(lb.getKey()).thenReturn("key123");
        when(leaderboardInfoRepository.findById("123")).thenReturn(Optional.of(lb));

        ZSetOperations.TypedTuple<String> tuple = mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn("1");
        when(tuple.getScore()).thenReturn(100.0);

        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        tuples.add(tuple);
        when(zSetOperations.reverseRangeWithScores("key123", 0, -1)).thenReturn(tuples);

        handler.onMessage(message, null);

        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }
}