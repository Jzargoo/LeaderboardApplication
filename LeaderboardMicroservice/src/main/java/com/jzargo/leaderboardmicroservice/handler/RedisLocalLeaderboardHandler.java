package com.jzargo.leaderboardmicroservice.handler;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import com.jzargo.messaging.UserLocalUpdateEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.springframework.data.redis.listener.Topic.pattern;

@Component
@Slf4j
public class RedisLocalLeaderboardHandler implements MessageListener {
    private final KafkaTemplate<String, UserLocalUpdateEvent> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RedisLocalLeaderboardHandler(
            KafkaTemplate<String, UserLocalUpdateEvent> kafkaTemplate,
            StringRedisTemplate stringRedisTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void subscription(RedisMessageListenerContainer container) {
        container.addMessageListener(this, pattern("leaderboard-cache:*:userId:*:local-leaderboard-update"));
        log.trace("Subscribed to user's local-leaderboard-update channel");

    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        int rankOld = Integer.parseInt(Arrays.toString(message.getBody()));
        log.info("Received local leaderboard update message with old rank: {}", rankOld);

        String[] split = Arrays.toString(pattern).split(":");
        Long l = stringRedisTemplate.opsForZSet().reverseRank(split[1], split[3]);
        if(l == null){
            log.warn("User with ID {} not found in leaderboard {}", split[3], split[1]);
            return;
        }

        Set<String> range = stringRedisTemplate.opsForZSet().range(split[1], rankOld, l - 1);
        List<UserLocalUpdateEvent.UserLocalEntry> list = new ArrayList<>(Objects.requireNonNull(range).stream().map(
                el -> {
                    Long rank = stringRedisTemplate.opsForZSet().rank(split[1], el);
                    return new UserLocalUpdateEvent.UserLocalEntry(Long.getLong(el), null, rank);
                }
        ).toList());

        Objects.requireNonNull(stringRedisTemplate.opsForZSet().reverseRangeWithScores(split[1], l, l)).forEach(
                el -> {
                    UserLocalUpdateEvent.UserLocalEntry userLocalEntry = new UserLocalUpdateEvent.UserLocalEntry(Long.getLong(el.getValue()), el.getScore(), l);
                    list.add(userLocalEntry);
                }
        );

        String messageId = UUID.randomUUID().toString();
        ProducerRecord<String, UserLocalUpdateEvent>
                record= new ProducerRecord<>(split[1], new UserLocalUpdateEvent(split[1],list));
        record.headers().add("message-id", messageId.getBytes());
        kafkaTemplate.send(record);
    }
}
