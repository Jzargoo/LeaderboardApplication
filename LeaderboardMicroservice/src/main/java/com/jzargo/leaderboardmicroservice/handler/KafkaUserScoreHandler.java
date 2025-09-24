package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import messaging.UserScoreEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Component
@Slf4j
@KafkaListener(topics = KafkaConfig.LEADERBOARD_EVENT_TOPIC, groupId = "leaderboard-group")
public class KafkaUserScoreHandler {
    private final StringRedisTemplate stringRedisTemplate;

    public KafkaUserScoreHandler(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    @KafkaHandler
    public void handleUserMutableChangeEvent(@Payload UserScoreEvent event,
                                              @Header (KafkaConfig.MESSAGE_ID) String messageId
                                              ) {
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent("processed:" + messageId, "1", Duration.ofDays(7));
        if(success != null && !success) {
            log.debug("Handled processed message with id {}", messageId);
            return;
        }


    }

    @KafkaHandler
    public void handleUserImmutableChangeEvent() {

    }

}
