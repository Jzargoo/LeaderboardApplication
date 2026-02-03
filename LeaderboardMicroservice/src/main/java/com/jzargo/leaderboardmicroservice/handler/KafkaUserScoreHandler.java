package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@KafkaListener(
        topics = "#{@kafkaPropertyStorage.topic.names.leaderboardEvent}",
        groupId = "#{@kafkaPropertyStorage.consumer.groupId}"
)
public class KafkaUserScoreHandler {

    private final StringRedisTemplate stringRedisTemplate;
    private final LeaderboardService leaderboardService;

    public KafkaUserScoreHandler(StringRedisTemplate stringRedisTemplate, LeaderboardService leaderboardService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.leaderboardService = leaderboardService;
    }

    @KafkaHandler
    public void handleUserMutableChangeEvent(@Payload UserScoreEvent event,
                                              @Header ("#{@kafkaPropertyStorage.headers.messageId}") String messageId
                                              ) {
        String key = "processed:" + messageId;

        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(7));
        if(success != null && !success) {
            log.warn("Handled processed message with id {}", messageId);
            return;
        }
        try {
            leaderboardService.increaseUserScore(event);
            log.info("Processed message with id {}", messageId);

        } catch (Exception e) {
            log.error("Failed to process message with id {}", messageId, e);
            stringRedisTemplate.delete(key);
        }

    }


    @KafkaHandler
    public void handleUserImmutableChangeEvent(@Payload UserScoreUploadEvent event,
                                               @Header ("#{@kafkaPropertyStorage.headers.messageId}") String messageId
                                               ) {

        String key = "processed:" + messageId;

        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(7));
        if(success != null && !success) {
            log.debug("Handled processed message with id {} in immutable changes", messageId);
            return;
        }

        try {
            leaderboardService.addNewScore(event);
            log.info("Processed message with id {} in immutable changes", messageId);
        } catch (Exception e) {
            log.error("Failed to process message with id {} in immutable changes", messageId, e);
            stringRedisTemplate.delete(key);
        }
    }

}
