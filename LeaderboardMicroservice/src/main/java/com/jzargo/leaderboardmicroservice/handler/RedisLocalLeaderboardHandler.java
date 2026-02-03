package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.messaging.UserLocalUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;


@Slf4j
@Component
public class RedisLocalLeaderboardHandler implements StreamListener<String, MapRecord<String, String, String>> {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;


    public RedisLocalLeaderboardHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            StringRedisTemplate stringRedisTemplate, KafkaPropertyStorage kafkaPropertyStorage) {
        this.kafkaTemplate = kafkaTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {

        long oldRank = Long.parseLong(message.getValue().get("oldRank"));
        String leaderboardKey =  message.getValue().get("leaderboardKey");
        String userId =  message.getValue().get("userId");
        String leaderboardId = message.getValue().get("lbId");

        Long l = stringRedisTemplate.opsForZSet().reverseRank(leaderboardKey, userId);
        if (l == null) {
            log.warn("User {} not found in leaderboard {}", userId, leaderboardKey);
            return;
        }

        Set<String> range = stringRedisTemplate.opsForZSet().range(leaderboardKey, oldRank, l - 1);
        ArrayList<UserLocalUpdateEvent.UserLocalEntry> list = new ArrayList<>(
                Objects.requireNonNull(range)
                        .stream().map(
                el -> {
                    Long rank = stringRedisTemplate.opsForZSet().rank(leaderboardKey, el);
                    return new UserLocalUpdateEvent.UserLocalEntry(Long.parseLong(el), null, rank);
                }
        ).toList());

        Set<ZSetOperations.TypedTuple<String>> last = stringRedisTemplate.opsForZSet().reverseRangeWithScores(leaderboardKey, l, l);
        if (last != null) {
            last.forEach(el -> {
                UserLocalUpdateEvent.UserLocalEntry entry = new UserLocalUpdateEvent.UserLocalEntry(
                        Long.parseLong(el.getValue()), el.getScore(), l
                );
                list.add(entry);
            });
        }

        String messageId = KafkaUtils.newMessageId();
        ProducerRecord<String, Object> record =
                KafkaUtils.createRecord(
                        kafkaPropertyStorage.getTopic().getNames().getLeaderboardUpdateState(),
                        leaderboardId,
                        new UserLocalUpdateEvent(leaderboardKey, list));

        record.headers()
                .add(kafkaPropertyStorage.getHeaders().getMessageId(), messageId.getBytes())
                .add(RECEIVED_KEY, leaderboardId.getBytes());

        kafkaTemplate.send(record);
    }
}
