package com.jzargo.leaderboardmicroservice.handler;

import lombok.extern.slf4j.Slf4j;
import com.jzargo.messaging.UserLocalUpdateEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;


@Component
@Slf4j
public class RedisLocalLeaderboardHandler {
    private final KafkaTemplate<String, UserLocalUpdateEvent> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String STREAM_KEY = "local-leaderboard-stream";
    private static final String GROUP_NAME = "local-consumer-group";


    public RedisLocalLeaderboardHandler(
            KafkaTemplate<String, UserLocalUpdateEvent> kafkaTemplate,
            StringRedisTemplate stringRedisTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            log.info("Consumer group might already exist: {}", e.getMessage());
        }
        new Thread(this::pollStream).start();
    }


    private void pollStream() {
        while (true) {
            List<MapRecord<String, Object, Object>> messages =
                    stringRedisTemplate.opsForStream().read(
                            Consumer.from(GROUP_NAME, "consumer-1"),
                            StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                    );

            if (messages != null) {
                for (MapRecord<String, Object, Object> message : messages) {
                    handleMessage(message);
                    stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, message.getId());
                }
            }
        }
    }

    private void handleMessage(MapRecord<String, Object, Object> message) {
        Long oldRank = (Long) message.getValue().get("oldRank");
        String leaderboardKey = (String) message.getValue().get("leaderboardKey");
        String userId = (String) message.getValue().get("userId");

        Long l = stringRedisTemplate.opsForZSet().reverseRank(leaderboardKey, userId);
        if (l == null) {
            log.warn("User {} not found in leaderboard {}", userId, leaderboardKey);
            return;
        }

        Set<String> range = stringRedisTemplate.opsForZSet().range(leaderboardKey, oldRank, l - 1);
        List<UserLocalUpdateEvent.UserLocalEntry> list = range.stream().map(
                el -> {
                    Long rank = stringRedisTemplate.opsForZSet().rank(leaderboardKey, el);
                    return new UserLocalUpdateEvent.UserLocalEntry(Long.parseLong(el), null, rank);
                }
        ).toList();

        Set<ZSetOperations.TypedTuple<String>> last = stringRedisTemplate.opsForZSet().reverseRangeWithScores(leaderboardKey, l, l);
        if (last != null) {
            last.forEach(el -> {
                UserLocalUpdateEvent.UserLocalEntry entry = new UserLocalUpdateEvent.UserLocalEntry(
                        Long.parseLong(el.getValue()), el.getScore(), l
                );
                list.add(entry);
            });
        }

        String messageId = UUID.randomUUID().toString();
        ProducerRecord<String, UserLocalUpdateEvent> record = new ProducerRecord<>(
                leaderboardKey, new UserLocalUpdateEvent(leaderboardKey, list)
        );
        record.headers().add("message-id", messageId.getBytes());
        kafkaTemplate.send(record);
    }
}
