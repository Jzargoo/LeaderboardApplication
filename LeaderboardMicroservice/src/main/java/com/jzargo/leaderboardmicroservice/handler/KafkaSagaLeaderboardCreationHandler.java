package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.saga.SagaUtils;
import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;


@Component
@KafkaListener(topics = {
        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
        KafkaConfig.LEADERBOARD_EVENT_TOPIC
},
        groupId = KafkaConfig.GROUP_ID
)
@Slf4j
public class KafkaSagaLeaderboardCreationHandler {

    private final StringRedisTemplate stringRedisTemplate;
    private final SagaLeaderboardCreate sagaLeaderboardCreate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LeaderboardInfoRepository leaderboardInfoRepository;

    public KafkaSagaLeaderboardCreationHandler(StringRedisTemplate stringRedisTemplate,
                                               SagaLeaderboardCreate sagaLeaderboardCreate,
                                               KafkaTemplate<String, Object> kafkaTemplate,
                                               LeaderboardInfoRepository leaderboardInfoRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sagaLeaderboardCreate = sagaLeaderboardCreate;
        this.kafkaTemplate = kafkaTemplate;
        this.leaderboardInfoRepository = leaderboardInfoRepository;
    }

    private static void getWarn(String messageId) {
        log.warn("Handled already-processed message with id {}", messageId);
    }

    @KafkaHandler
    public void handleCreateLeaderboardSaga(InitLeaderboardCreateEvent event,
                                            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
                                            @Header(KafkaConfig.MESSAGE_ID) String messageId
    ){
        log.info("The body is {}", event);
        if (!SagaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
            getWarn(messageId);
            return;
        }
        try {
            boolean b = sagaLeaderboardCreate.stepCreateLeaderboard(event, sagaId);
            if (!b) {
                log.error("Error while processing create leaderboard message {}", messageId);
            }
        } catch (Exception e) {
            log.error("Error while processing create leaderboard message {}", messageId, e);
        }
    }

    @KafkaHandler
    @Transactional
    public void handleSuccessfulEventInitialization(
            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
            @Header(KafkaConfig.MESSAGE_ID) String messageId,
            @Payload SuccessfulEventInitialization successfulEventInitialization
    ) {
        if (!SagaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
            getWarn(messageId);
            return;
        }

        try {
            sagaLeaderboardCreate.stepSuccessfulEventInit(successfulEventInitialization, sagaId);

            LeaderboardInfo byId = leaderboardInfoRepository.findById(successfulEventInitialization.getLbId())
                    .orElseThrow(() -> new IllegalArgumentException("lb id cannot be null"));

            UserNewLeaderboardCreated userNewLeaderboardCreated = new UserNewLeaderboardCreated(
                    successfulEventInitialization.getLbId(), byId.getName(),
                    byId.getOwnerId()
            );
            ProducerRecord<String, Object> record = SagaUtils.createRecord(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                    byId.getId(), userNewLeaderboardCreated);

            SagaUtils.addSagaHeaders(record, sagaId, SagaUtils.newMessageId(), byId.getId());
            kafkaTemplate.send(record);

            log.info("Handled SuccessfulEventInitialization (sagaId={})", sagaId);
        } catch (IllegalArgumentException e) {
            log.error("Incorrect event without lb id or bad payload, sagaId={}", sagaId, e);
            SagaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        } catch (Exception e) {
            log.error("Error while processing SuccessfulEventInitialization message {}", messageId, e);
            SagaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        }
    }

    @KafkaHandler
    @Transactional
    public void handleUserAddedLeaderboard(
            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
            @Header(KafkaConfig.MESSAGE_ID) String messageId,
            @Payload UserAddedLeaderboard userAddedLeaderboard
    ) {
        if (!SagaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
            getWarn(messageId);
            return;
        }

        try {
            sagaLeaderboardCreate.stepSagaCompleted(userAddedLeaderboard, sagaId);
            log.info("Saga completed for sagaId={}", sagaId);
        } catch (IllegalArgumentException e) {
            log.error("Leaderboard id incorrect or other business validation failed for sagaId={}", sagaId, e);
            SagaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        } catch (Exception e) {
            log.error("Error while processing UserAddedLeaderboard message {}", messageId, e);
            SagaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        }
    }

    @KafkaHandler
    @Transactional
    public void handleFailedCreation(
            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
            @Header(KafkaConfig.MESSAGE_ID) String messageId,
            @Payload FailedLeaderboardCreation failed
    ) {
        if (!SagaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
            getWarn(messageId);
            return;
        }

        try {
            FailedLeaderboardCreation.SourceOfFail source = failed.getSourceOfFail();

            if (source == FailedLeaderboardCreation.SourceOfFail.EVENTS) {
                log.info("Saga {} failed at OPTIONAL EVENTS step", sagaId);

                sagaLeaderboardCreate.compensateStepOptionalEvent(sagaId, failed.getLbId());

            }else if (source == FailedLeaderboardCreation.SourceOfFail.USER_PROFILE) {
                log.info("Saga {} failed at USER PROFILE step", sagaId);


            }
            log.info("Compensation workflow started for saga {}", sagaId);
        } catch (Exception e) {
            log.error("Error while handling failed creation for saga {} message {}", sagaId, messageId, e);
        }
    }

    @KafkaHandler
    public void handleDeletedEvents(
            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
            @Header(KafkaConfig.MESSAGE_ID) String messageId,
            @Payload LeaderboardEventDeletion led
    ){
        if (!SagaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
            getWarn(messageId);
            return;
        }
        try {
            sagaLeaderboardCreate.compensateStepOptionalEvent(sagaId, led.getLbId());
        } catch (Exception e) {
            log.error("Error while processing deletion events {}", led.getLbId(), e);
        }
    }


    @KafkaHandler
    public void handleDeleteLbEvent(
            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
            @Header(KafkaConfig.MESSAGE_ID) String messageId,
            @Payload DeleteLbEvent dle
    ){
        if (!SagaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
            getWarn(messageId);
            return;
        }
        try {
            sagaLeaderboardCreate.stepCompensateLeaderboard(dle, sagaId);
        } catch (Exception e) {
            log.error("Error while processing deletion events {}", dle.getLbId(), e);
        }
    }

}