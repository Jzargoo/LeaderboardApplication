package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.messaging.GlobalLeaderboardEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

@Component
@Slf4j
public class RedisGlobalLeaderboardUpdateHandler implements StreamListener<String, MapRecord<String, String, String>> {
    private final KafkaTemplate<String, GlobalLeaderboardEvent> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;


    public RedisGlobalLeaderboardUpdateHandler(KafkaTemplate<String, GlobalLeaderboardEvent> kafkaTemplate, StringRedisTemplate stringRedisTemplate, KafkaPropertyStorage kafkaPropertyStorage) {
        this.kafkaTemplate = kafkaTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message){
        String leaderboardKey = message.getValue().get("lbKey");
        int maxTop = Integer.parseInt(message.getValue().get("maxTop"));
        String lbId = message.getValue().get("lbId");


        ArrayList<GlobalLeaderboardEvent.Entry> top = new ArrayList<>();
        Objects.requireNonNull(stringRedisTemplate.opsForZSet().reverseRangeWithScores(leaderboardKey, 0, maxTop - 1))
                .forEach(tuple -> {
                    if(tuple.getValue() == null || tuple.getScore() == null) return;
                    GlobalLeaderboardEvent.Entry entry = new GlobalLeaderboardEvent.Entry();
                    entry.setUserId(Long.parseLong(tuple.getValue()));
                    entry.setScore(tuple.getScore());
                    top.add(entry);
                });
        GlobalLeaderboardEvent build = GlobalLeaderboardEvent.builder()
                .id(lbId)
                .topNLeaderboard(top)
                .createdAt(LocalDateTime.now())
                .build();
        ProducerRecord<String, GlobalLeaderboardEvent> record =
                new ProducerRecord<>(kafkaPropertyStorage.getTopic().getNames().getLeaderboardUpdateState(), lbId, build);
        record.headers().add(
                kafkaPropertyStorage.getHeaders().getMessageId(),
                        message.getId().getValue().getBytes()
                )
                .add(
                        RECEIVED_KEY,
                        message.getId().getValue().getBytes()
                );

        kafkaTemplate.send(record);
    }
}
