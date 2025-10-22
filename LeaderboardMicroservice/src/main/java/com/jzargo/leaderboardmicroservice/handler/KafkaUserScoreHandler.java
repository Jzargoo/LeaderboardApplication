package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.UserUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@KafkaListener(topics = {
        KafkaConfig.LEADERBOARD_EVENT_TOPIC,
        KafkaConfig.USER_STATE_EVENT_TOPIC
},
        groupId = "leaderboard-group"
)
@Component
public class KafkaUserScoreHandler {
    private final StringRedisTemplate stringRedisTemplate;
    private final LeaderboardService leaderboardService;

    public KafkaUserScoreHandler(StringRedisTemplate stringRedisTemplate, LeaderboardService leaderboardService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.leaderboardService = leaderboardService;
    }

    @KafkaHandler
    public void handleUserMutableChangeEvent(@Payload UserScoreEvent event,
                                              @Header (KafkaConfig.MESSAGE_ID) String messageId
                                              ) {
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent("processed:" + messageId, "1", Duration.ofDays(7));
        if(success != null && !success) {
            log.warn("Handled processed message with id {}", messageId);
            return;
        }
        try {
            leaderboardService.increaseUserScore(event);
            log.info("Processed message with id {}", messageId);

        } catch (Exception e) {
            log.error("Failed to process message with id {}", messageId, e);
        }

    }


    @KafkaHandler
    public void handleUserImmutableChangeEvent(@Payload UserScoreUploadEvent event,
                                               @Header(KafkaConfig.MESSAGE_ID) String messageId
                                               ) {
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent("processed:" + messageId, "1", Duration.ofDays(7));
        if(success != null && !success) {
            log.debug("Handled processed message with id {} in immutable changes", messageId);
            return;
        }

        try {
            leaderboardService.addNewScore(event);
            log.info("Processed message with id {} in immutable changes", messageId);
        } catch (Exception e) {
            log.error("Failed to process message with id {} in immutable changes", messageId, e);
        }
    }

    @KafkaHandler
    public void handleUserUpdateEvent(@Payload UserUpdateEvent userUpdateEvent,
                              @Header(KafkaConfig.MESSAGE_ID) String messageId) {
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent("processed:" + messageId, "1", Duration.ofDays(7));
        if(success != null && !success) {
            log.debug("Handled processed message with id {} in updating user's cache", messageId);
            return;
        }

        try {
            leaderboardService.updateUserCache(userUpdateEvent);
            log.info("Processed message with id {} in updating user's cache", messageId);
        } catch (Exception e) {
            log.error("Failed to process message with id {} in updating user's cache", messageId, e);
        }

    }
}
