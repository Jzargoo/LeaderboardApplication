package com.jzargo.usermicroservice.handler;

import com.jzargo.messaging.ActiveLeaderboardEvent;
import com.jzargo.usermicroservice.config.KafkaConfig;
import com.jzargo.usermicroservice.repository.ProcessingMessageRepository;
import com.jzargo.usermicroservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@KafkaListener(topics = {KafkaConfig.PULSE_LEADERBOARD})
public class UserKafkaHandler {

    private final ProcessingMessageRepository processingMessageRepository;
    private final UserService userService;

    public UserKafkaHandler(ProcessingMessageRepository processingMessageRepository, UserService userService) {
        this.processingMessageRepository = processingMessageRepository;
        this.userService = userService;
    }

    @Transactional
    @KafkaHandler
    public void activeLeaderboard(
            @Header("message-id") String messageId,
            @Payload ActiveLeaderboardEvent event){
        if(processingMessageRepository.existsById(messageId)){
            log.warn("That message already had been processed");
            return;
        }

        userService.addActiveLeaderboard(event);
    }
}
