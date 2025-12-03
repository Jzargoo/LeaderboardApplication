package com.jzargo.scoringmicroservice.handlers;

import com.jzargo.scoringmicroservice.config.KafkaConfig;
import com.jzargo.scoringmicroservice.entity.DeletedEvents;
import com.jzargo.scoringmicroservice.entity.FailedCreateLeaderboardEvents;
import com.jzargo.scoringmicroservice.entity.ProcessedMessage;
import com.jzargo.scoringmicroservice.entity.SuccessfulEventsCreation;
import com.jzargo.scoringmicroservice.repository.DeletedEventsRepository;
import com.jzargo.scoringmicroservice.repository.FailedCreateLeaderboardEventsRepository;
import com.jzargo.scoringmicroservice.repository.ProcessedMessageRepository;
import com.jzargo.scoringmicroservice.service.ScoringService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.jzargo.messaging.LeaderboardEventDeletion;
import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.UserEventHappenedCommand;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@KafkaListener(
        topics = {
                KafkaConfig.COMMAND_STRING_SCORE_TOPIC,
                KafkaConfig.USER_EVENT_SCORE_TOPIC,
                KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC
        },
        groupId = KafkaConfig.GROUP_ID
)
public class KafkaScoringEventsAndCommandsHandler {
    private final ProcessedMessageRepository processedMessageRepository;
    private final ScoringService scoringService;
    private final FailedCreateLeaderboardEventsRepository failedCreateLeaderboardEventsRepository;
    private final DeletedEventsRepository deletedEventsRepository;

    public KafkaScoringEventsAndCommandsHandler(ProcessedMessageRepository processedMessageRepository, ScoringService scoringService, FailedCreateLeaderboardEventsRepository failedCreateLeaderboardEventsRepository, DeletedEventsRepository deletedEventsRepository) {
        this.processedMessageRepository = processedMessageRepository;
        this.scoringService = scoringService;
        this.failedCreateLeaderboardEventsRepository = failedCreateLeaderboardEventsRepository;
        this.deletedEventsRepository = deletedEventsRepository;
    }

    @KafkaHandler
    @Transactional
    public void handleStringScoreCommand(@Payload UserEventHappenedCommand command,
                                         @Header  (KafkaConfig.MESSAGE_HEADER) String messageId){
        if(processedMessageRepository.existsById(messageId)) {
            getDebug(messageId);
            return;
        }
        try {
            log.info("Handling command: {}", command);
            scoringService.saveUserEvent(command);
        } finally {
            processedMessageRepository.save(
                ProcessedMessage.builder()
                        .id(messageId)
                        .messageType("command")
                        .build()
            );
        }
    }

    private static void getDebug(String messageId) {
        log.debug("Handled processed message with id {}", messageId);
    }

    @KafkaHandler
    @Transactional
    public void handleCreateLeaderboardEvents (@Payload LeaderboardEventInitialization leaderboardEventInitialization,
                                              @Header(KafkaConfig.SAGA_HEADER) String sagaId,
                                              @Header(KafkaConfig.MESSAGE_HEADER) String messageId){
        if (processedMessageRepository.existsById(messageId)) {
            getDebug(messageId);
            return;
        }
        try{
            log.info("Handling initialization: {}", leaderboardEventInitialization);
            scoringService.saveEvents(leaderboardEventInitialization);
            new SuccessfulEventsCreation(
                 sagaId,
                 leaderboardEventInitialization.getLbId(),
                 sagaId,
                 leaderboardEventInitialization.getUserId()
            );
        } catch (Exception e){
            failedCreateLeaderboardEventsRepository.save(
                    new FailedCreateLeaderboardEvents(
                            UUID.randomUUID().toString(),
                            leaderboardEventInitialization.getLbId(),
                            e.getMessage(),
                            sagaId,
                            leaderboardEventInitialization.getUserId()
                    )
            );
        }finally {
            processedMessageRepository.save(
                    ProcessedMessage.builder()
                            .id(messageId)
                            .messageType("command")
                            .build()
            );
        }
    }

    @KafkaHandler
    @Transactional
    public void handleDeleteLeaderboardEvents(@Payload LeaderboardEventDeletion deletion,
                                              @Header(KafkaConfig.SAGA_HEADER) String sagaId,
                                              @Header(KafkaConfig.MESSAGE_HEADER) String messageId){
        if (processedMessageRepository.existsById(messageId)) {
            getDebug(messageId);
            return;
        }
        try{
            log.info("Handling deletion: {}", deletion.getLbId());
            scoringService.deleteEvents(deletion);
            deletedEventsRepository.save(
                    new DeletedEvents(null,deletion.getLbId(), sagaId)
            );
        } catch (Exception e){
            log.error("Error deleting leaderboard events for lbId {}: {}", deletion.getLbId(), e.getMessage());
        }finally {
            processedMessageRepository.save(
                    ProcessedMessage.builder()
                            .id(messageId)
                            .messageType("command")
                            .build()
            );
        }
    }
}