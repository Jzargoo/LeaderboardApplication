package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@KafkaListener(topics = {
        "#{@kafkaPropertyStorage.topic.names.leaderboardEvent}",
        "#{@kafkaPropertyStorage.topic.names.sagaCreateLeaderboard}"
},
        groupId = "#{@kafkaPropertyStorage.consumer.groupId}"
)
@Slf4j
public class KafkaSagaLeaderboardCreationHandler {

    private final StringRedisTemplate stringRedisTemplate;
    private final SagaLeaderboardCreate sagaLeaderboardCreate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LeaderboardInfoRepository leaderboardInfoRepository;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    public KafkaSagaLeaderboardCreationHandler(StringRedisTemplate stringRedisTemplate,
                                               SagaLeaderboardCreate sagaLeaderboardCreate,
                                               KafkaTemplate<String, Object> kafkaTemplate,
                                               LeaderboardInfoRepository leaderboardInfoRepository, KafkaPropertyStorage kafkaPropertyStorage) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sagaLeaderboardCreate = sagaLeaderboardCreate;
        this.kafkaTemplate = kafkaTemplate;
        this.leaderboardInfoRepository = leaderboardInfoRepository;
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    private static void getWarn(String messageId) {
        log.warn("Handled already-processed message with id {}", messageId);
    }

    @KafkaHandler
    public void handleCreateLeaderboardSaga(InitLeaderboardCreateEvent event,
                                            @Header("#{@kafkaPropertyStorage.headers.sagaId}") String sagaId,
                                            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId
    ){
        log.info("The body is {}", event);
        if (!KafkaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
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
            @Header("#{@kafkaPropertyStorage.headers.sagaId}") String sagaId,
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId,
            @Payload SuccessfulEventInitialization successfulEventInitialization
    ) {
        if (!KafkaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
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
            ProducerRecord<String, Object> record = KafkaUtils.createRecord(kafkaPropertyStorage.getTopic().getNames().getLeaderboardEvent(),
                    byId.getId(), userNewLeaderboardCreated);

            KafkaUtils.addSagaHeaders(
                    record, sagaId,
                    byId.getId(),
                    kafkaPropertyStorage.getHeaders().getMessageId(),
                    kafkaPropertyStorage.getHeaders().getSagaId()
            );
            kafkaTemplate.send(record);

            log.info("Handled SuccessfulEventInitialization (sagaId={})", sagaId);
        } catch (IllegalArgumentException e) {
            log.error("Incorrect event without lb id or bad payload, sagaId={}", sagaId, e);
            KafkaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        } catch (Exception e) {
            log.error("Error while processing SuccessfulEventInitialization message {}", messageId, e);
            KafkaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        }
    }

    @KafkaHandler
    @Transactional
    public void handleUserAddedLeaderboard(
            @Payload UserAddedLeaderboard userAddedLeaderboard,
            @Header("#{@kafkaPropertyStorage.headers.sagaId}") String sagaId,
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId
    ) {
        if (!KafkaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
            getWarn(messageId);
            return;
        }

        try {
            sagaLeaderboardCreate.stepSagaCompleted(userAddedLeaderboard, sagaId);
            log.info("Saga completed for sagaId={}", sagaId);
        } catch (IllegalArgumentException e) {
            log.error("Leaderboard id incorrect or other business validation failed for sagaId={}", sagaId, e);
            KafkaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        } catch (Exception e) {
            log.error("Error while processing UserAddedLeaderboard message {}", messageId, e);
            KafkaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        }
    }

    @KafkaHandler
    @Transactional
    public void handleFailedCreation(
            @Payload FailedLeaderboardCreation failed,
            @Header("#{@kafkaPropertyStorage.headers.sagaId}") String sagaId,
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId
            ) {
        if (!KafkaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
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

            @Header("#{@kafkaPropertyStorage.headers.sagaId}") String sagaId,
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId,
            @Payload LeaderboardEventDeletion led
    ){
        if (!KafkaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
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
            @Header("#{@kafkaPropertyStorage.headers.sagaId}") String sagaId,
            @Header("#{@kafkaPropertyStorage.headers.messageId}") String messageId,
            @Payload DeleteLbEvent dle
    ){
        if (!KafkaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
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