package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@KafkaListener(
        topics = "#{@kafkaPropertyStorage.topic.names.sagaCreateLeaderboard}",
        groupId = "#{@kafkaPropertyStorage.consumer.groupId}"
)
@Slf4j
public class KafkaSagaLeaderboardCreationHandler {

    private final StringRedisTemplate stringRedisTemplate;
    private final SagaLeaderboardCreate sagaLeaderboardCreate;

    public KafkaSagaLeaderboardCreationHandler(
            StringRedisTemplate stringRedisTemplate,
            SagaLeaderboardCreate sagaLeaderboardCreate
    ){

        this.stringRedisTemplate = stringRedisTemplate;
        this.sagaLeaderboardCreate = sagaLeaderboardCreate;

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
            log.info("The message {} was successfully processed in step create leaderboard", event);
        } catch (Exception e) {
            log.error("Error while processing create leaderboard message {}", messageId, e);
        }
    }

    @KafkaHandler
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

            log.trace("Handled message with successful event init");

            sagaLeaderboardCreate.stepSuccessfulEventInit(successfulEventInitialization, sagaId);

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
            log.trace("Handled message that user profile added leaderboard");

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

            log.warn("Handled message that creation leaderboard was failed");

            FailedLeaderboardCreation.SourceOfFail source = failed.getSourceOfFail();

            if (source == FailedLeaderboardCreation.SourceOfFail.EVENTS) {
                log.debug("Saga {} failed at OPTIONAL EVENTS step", sagaId);

                sagaLeaderboardCreate.compensateStepOptionalEvent(sagaId, failed.getLbId());

            } else if (source == FailedLeaderboardCreation.SourceOfFail.USER_PROFILE) {
                log.debug("Saga {} failed at USER PROFILE step", sagaId);

                sagaLeaderboardCreate.compensateStepUserProfile(sagaId, failed);
            }

            log.info("Compensation workflow started for saga {}", sagaId);
        } catch (Exception e) {

            log.error("Error while handling failed creation for saga {} message {}", sagaId, messageId, e);

            KafkaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
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
            log.trace("handled leaderboard event deletion");

            sagaLeaderboardCreate.compensateStepOptionalEvent(sagaId, led.getLbId());

            log.info("message about deleted events with saga id {} was processed successfully", sagaId);

        } catch (Exception e) {
            log.error("Error while processing deletion events {}", led.getLbId(), e);
            KafkaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
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

            log.trace("handled step compensate leaderboard for saga id {}", sagaId);

            sagaLeaderboardCreate.stepCompensateLeaderboard(dle, sagaId);

            log.info("step compensate leaderboard for saga id {} processed successfully", sagaId);

        } catch (Exception e) {
            log.error("Error while processing deletion events {}", dle.getLbId(), e);
            KafkaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
        }
    }

}