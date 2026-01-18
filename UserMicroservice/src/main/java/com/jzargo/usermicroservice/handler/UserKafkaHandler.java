package com.jzargo.usermicroservice.handler;

import com.jzargo.messaging.ActiveLeaderboardEvent;
import com.jzargo.messaging.DiedLeaderboardEvent;
import com.jzargo.messaging.OutOfTimeEvent;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import com.jzargo.usermicroservice.entity.FailedLeaderboardCreation;
import com.jzargo.usermicroservice.entity.ProcessingMessage;
import com.jzargo.usermicroservice.entity.UserAddedCreatedLeaderboard;
import com.jzargo.usermicroservice.exception.UserCannotCreateLeaderboardException;
import com.jzargo.usermicroservice.repository.FailedLeaderboardCreationRepository;
import com.jzargo.usermicroservice.repository.ProcessingMessageRepository;
import com.jzargo.usermicroservice.repository.UserAddedCreatedLeaderboardRepository;
import com.jzargo.usermicroservice.service.UserService;
import com.sun.jdi.request.DuplicateRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
@Slf4j
@KafkaListener(
        topics = {
                "#{@kafkaPropertiesStorage.topic.names.participantLeaderboard}",
                "#{@kafkaPropertiesStorage.topic.names.sagaCreateLeaderboard}"
        },
        groupId = "#{@kafkaPropertiesStorage.consumer.groupId}"
)
@RequiredArgsConstructor
public class UserKafkaHandler {

    private static final String PROCESSED_EVENT_MESSAGE = "That message already had been processed";

    private final ProcessingMessageRepository processingMessageRepository;
    private final UserService userService;
    private final FailedLeaderboardCreationRepository failedLeaderboardCreationRepository;
    private final UserAddedCreatedLeaderboardRepository userAddedCreatedLeaderboardRepository;

    @Transactional
    @KafkaHandler
    public void activeLeaderboard(
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId,
            @Payload ActiveLeaderboardEvent event){
        checkProcessed(messageId);
        try {
            userService.addActiveLeaderboard(event);
        } finally {
            processingMessageRepository.save(
                ProcessingMessage.builder()
                        .id(messageId)
                        .type("Event")
                        .build()
            );
        }

    }

    @Transactional
    @KafkaHandler
    public void endLeaderboard(
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId,
            @Payload DiedLeaderboardEvent event){
        checkProcessed(messageId);
        try {
            userService.removeLeaderboard(event);
        } finally {
            processingMessageRepository.save(
                ProcessingMessage.builder()
                        .id(messageId)
                        .type("Event")
                        .build()
            );
        }

    }

    @Transactional
    @KafkaHandler
    public void handleOutOfTime(
            @Payload OutOfTimeEvent outOfTimeEvent,
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId
    ) {
        checkProcessed(messageId);

        try {
            userService.removeCreatedLeaderboard(outOfTimeEvent);
        } catch (Exception e){
            log.error("Leaderboard must not exist in user space", e);
        } finally {
            processingMessageRepository.save(
                    ProcessingMessage.builder()
                            .id(messageId)
                            .type("Event")
                            .build()
            );
        }
    }

    private void checkProcessed(String messageId) {
        if (processingMessageRepository.existsById(messageId)) {
            log.warn(PROCESSED_EVENT_MESSAGE);
            throw new DuplicateRequestException();
        }
    }

    @Transactional
    @KafkaHandler
    public void handleSaga(
            @Payload UserNewLeaderboardCreated userNewLeaderboardCreated,
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId,
            @Header("#{@kafkaPropertyStorage.headers.sagaId}") String sagaId
            ) {

        checkProcessed(messageId);

        try{
            userService.addCreatedLeaderboard(userNewLeaderboardCreated);
            UserAddedCreatedLeaderboard userAddedCreatedLeaderboard =
                    new UserAddedCreatedLeaderboard(
                            sagaId, sagaId,
                            userNewLeaderboardCreated.getUserId(),
                            userNewLeaderboardCreated.getLbId());
            userAddedCreatedLeaderboardRepository.save(
                    userAddedCreatedLeaderboard
            );
            processingMessageRepository.save(
                    ProcessingMessage.builder()
                            .id(messageId)
                            .type("Event")
                            .build()
            );

        } catch (UserCannotCreateLeaderboardException e) {
            failedLeaderboardCreationRepository.save(
                    new FailedLeaderboardCreation(
                            null,
                            e.getMessage(),
                            userNewLeaderboardCreated.getLbId(),
                            userNewLeaderboardCreated.getUserId(),
                            sagaId
                    )
            );
            processingMessageRepository.save(
                    ProcessingMessage.builder()
                            .id(messageId)
                            .type("Command")
                            .build()
            );
        }
    }
}