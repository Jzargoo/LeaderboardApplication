package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.messaging.GlobalLeaderboardEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
@Slf4j
public class RedisGlobalLeaderboardUpdateHandler{
    private final KafkaTemplate<String, GlobalLeaderboardEvent> kafkaTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String STREAM_KEY = "global-leaderboard-stream";
    private static final String GROUP_NAME = "global-consumer-group";


    public RedisGlobalLeaderboardUpdateHandler(KafkaTemplate<String, GlobalLeaderboardEvent> kafkaTemplate, StringRedisTemplate stringRedisTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        try{
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            log.info("Consumer group might already exist: {}", e.getMessage());
        }
        new Thread(this::pollStream).start();
    }

    public void pollStream(){
        while (true){
            var messages = stringRedisTemplate.opsForStream().read(
                    org.springframework.data.redis.connection.stream.Consumer.from(GROUP_NAME, "consumer-1"),
                    org.springframework.data.redis.connection.stream.StreamReadOptions.empty().count(10).block(java.time.Duration.ofSeconds(2)),
                    org.springframework.data.redis.connection.stream.StreamOffset.create(STREAM_KEY, org.springframework.data.redis.connection.stream.ReadOffset.lastConsumed())
            );
            if(messages != null){
                for(var message : messages){
                    handleMessage(message);
                    stringRedisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, message.getId());
                }
            }
        }
    }

    private void handleMessage(MapRecord<String, Object, Object> message){
        String leaderboardKey = (String) message.getValue().get("lbKey");
        Integer maxTop = (Integer) message.getValue().get("maxTop");
        String lbId = (String) message.getValue().get("lbId");


        ArrayList<GlobalLeaderboardEvent.Entry> top = new ArrayList<>();
        stringRedisTemplate.opsForZSet().reverseRangeWithScores(leaderboardKey, 0, maxTop - 1)
                .forEach(tuple -> {
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
                new ProducerRecord<>(KafkaConfig.LEADERBOARD_UPDATE_TOPIC, lbId, build);

        record.headers().add(
                KafkaConfig.MESSAGE_ID,
                        message.getId().getValue().getBytes()
                )
                .add(
                        KafkaHeaders.RECEIVED_KEY,
                        message.getId().getValue().getBytes()
                );

        kafkaTemplate.send(record);
    }
}
