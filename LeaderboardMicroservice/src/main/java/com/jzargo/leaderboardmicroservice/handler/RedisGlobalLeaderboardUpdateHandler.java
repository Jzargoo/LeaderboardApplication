package com.jzargo.leaderboardmicroservice.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.dto.GlobalLeaderboardCache;
import com.jzargo.leaderboardmicroservice.exceptions.IllegalPublicStateException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import messaging.GlobalLeaderboardEvent;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.data.redis.listener.Topic.pattern;

@Component
@Slf4j
public class RedisGlobalLeaderboardUpdateHandler implements MessageListener {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, GlobalLeaderboardEvent> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisGlobalLeaderboardUpdateHandler(ObjectMapper objectMapper,
                                               KafkaTemplate<String, GlobalLeaderboardEvent> kafkaTemplate,
                                               StringRedisTemplate stringRedisTemplate) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        log.info("Received leaderboard update message: {}", body);
        try {
            GlobalLeaderboardCache globalLeaderboardCache =
                    objectMapper.readValue(body, GlobalLeaderboardCache.class);

            Set<ZSetOperations.TypedTuple<String>> typedTuples =
                    stringRedisTemplate.opsForZSet()
                            .reverseRangeWithScores(pattern.toString(), 0, -1);

            Set<ZSetOperations.TypedTuple<String>> collect = globalLeaderboardCache
                    .getPayload().stream()
                    .map(e ->
                            new DefaultTypedTuple<>(
                                    e.getUserId() + "", e.getScore()))
                    .collect(Collectors.toSet());

            if (!Objects.equals(typedTuples, collect)) {
                stringRedisTemplate.opsForZSet()
                        .removeRange(pattern.toString(), 0, -1);
                stringRedisTemplate.opsForZSet()
                        .add(Arrays.toString(pattern), collect);
                log.info("Updated leaderboard {} in Redis", globalLeaderboardCache.getLeaderboardId());

                GlobalLeaderboardEvent event = GlobalLeaderboardEvent.builder()
                        .id(globalLeaderboardCache.getLeaderboardId())
                        .topNLeaderboard(
                                globalLeaderboardCache.getPayload().stream()
                                        .map(e ->
                                                new GlobalLeaderboardEvent.Entry(
                                                        e.getUserId(), e.getScore()))
                                        .toList()
                        )
                        .build();

                kafkaTemplate.send(KafkaConfig.LEADERBOARD_UPDATE_TOPIC, event);
                log.info("Published leaderboard {} update event to Kafka", globalLeaderboardCache.getLeaderboardId());

            } else {
                log.info("No changes detected for leaderboard {}, skipping update", globalLeaderboardCache.getLeaderboardId());
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse leaderboard update message: {}", body, e);
        }

    }

    @PostConstruct
    public void subscribing(RedisMessageListenerContainer container) {
        container.addMessageListener(this, pattern("leaderboard-cache:*:top*Leaderboard"));
        log.trace("Subscribed to Redis channel with global leaderboard update pattern");
    }
}
