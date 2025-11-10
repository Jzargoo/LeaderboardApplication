package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaStep;
import com.jzargo.leaderboardmicroservice.saga.SagaUtils;
import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import java.util.Map;
import java.util.Optional;


@Component
@KafkaListener(topics = {
        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
        KafkaConfig.GROUP_ID
})
@Slf4j
public class KafkaSagaLeaderboardCreationHandler {

    private final StringRedisTemplate stringRedisTemplate;
    private final SagaLeaderboardCreate sagaLeaderboardCreate;
    private final SagaControllingStateRepository sagaControllingStateRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LeaderboardInfoRepository leaderboardInfoRepository;

    public KafkaSagaLeaderboardCreationHandler(StringRedisTemplate stringRedisTemplate,
                                               SagaLeaderboardCreate sagaLeaderboardCreate,
                                               SagaControllingStateRepository sagaControllingStateRepository,
                                               KafkaTemplate<String, Object> kafkaTemplate,
                                               LeaderboardInfoRepository leaderboardInfoRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sagaLeaderboardCreate = sagaLeaderboardCreate;
        this.sagaControllingStateRepository = sagaControllingStateRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.leaderboardInfoRepository = leaderboardInfoRepository;
    }

    private static void getWarn(String messageId) {
        log.warn("Handled already-processed message with id {}", messageId);
    }

    @KafkaHandler
    public void handleCreateLeaderboardSaga(InitLeaderboardCreateEvent event,
                                            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
                                            @Header(KafkaConfig.MESSAGE_ID) String messageId,
                                            @Header(KafkaHeaders.RECEIVED_KEY) String region
    ){
        if (!SagaUtils.tryAcquireProcessingLock(stringRedisTemplate, messageId)) {
            getWarn(messageId);
            return;
        }

        try {
            SagaControllingState sagaControllingState = sagaControllingStateRepository.findById(sagaId)
                    .orElseThrow(() -> new IllegalArgumentException("Saga state not found: " + sagaId));

            boolean isMutable = sagaLeaderboardCreate.stepCreateLeaderboard(event, region, sagaId);

            ProducerRecord<String, Object> record;
            String partitionKey = sagaControllingState.getLeaderboardId();

            if (isMutable) {
                LeaderboardEventInitialization build = LeaderboardEventInitialization.builder()
                        .lbId(sagaControllingState.getLeaderboardId())
                        .events(event.getEvents())
                        .userId(event.getOwnerId())
                        .isPublic(event.isPublic())
                        .metadata(Map.of("expire_date", event.getExpireAt()))
                        .build();

                record = SagaUtils.createRecord(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC, partitionKey, build);
            } else {
                UserNewLeaderboardCreated userNewLeaderboardCreated = new UserNewLeaderboardCreated(
                        sagaControllingState.getLeaderboardId(),
                        event.getNameLb(),
                        event.getOwnerId()
                );
                record = SagaUtils.createRecord(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC, partitionKey, userNewLeaderboardCreated);
            }

            SagaUtils.addSagaHeaders(record, sagaId, SagaUtils.newMessageId(), partitionKey);
            kafkaTemplate.send(record);

            log.info("Processed saga create leaderboard (sagaId={})", sagaId);

        } catch (Exception e) {
            log.error("Error while processing create leaderboard message {}", messageId, e);

            try {
                Optional<SagaControllingState> maybe = sagaControllingStateRepository.findById(sagaId);
                if (maybe.isEmpty() || maybe.get().getLeaderboardId() == null) {
                    SagaUtils.releaseProcessingLock(stringRedisTemplate, messageId);
                } else {
                    SagaControllingState s = maybe.get();
                    s.setStatus(SagaStep.FAILED);
                    sagaControllingStateRepository.save(s);
                }
            } catch (Exception ex) {
                log.error("Error while trying to cleanup idempotency lock for message {}", messageId, ex);
            }
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
            String leaderboardId = failed.getLbId();

            if (source == FailedLeaderboardCreation.SourceOfFail.EVENTS) {
                log.info("Saga {} failed at OPTIONAL EVENTS step", sagaId);

                sagaLeaderboardCreate.compensateStepOptionalEvent(sagaId, failed);

                sendCompensateDeleteLeaderboard(leaderboardId, sagaId);

            }else if (source == FailedLeaderboardCreation.SourceOfFail.USER_PROFILE) {
                log.info("Saga {} failed at USER PROFILE step", sagaId);

                sagaLeaderboardCreate.compensateStepUserProfile(sagaId, failed);

                sendCompensateDeleteLeaderboard(leaderboardId, sagaId);

            }
            log.info("Compensation workflow started for saga {}", sagaId);
        } catch (Exception e) {
            log.error("Error while handling failed creation for saga {} message {}", sagaId, messageId, e);
        }
    }

    private void sendCompensateDeleteLeaderboard(String leaderboardId, String sagaId) {
        if (leaderboardId == null) {
            log.info("Saga {}: no leaderboard created yet — skip delete compensation", sagaId);
            return;
        }

        CompensateDeleteLeaderboardEvent comp = new CompensateDeleteLeaderboardEvent(leaderboardId);
        ProducerRecord<String, Object> rec =
                SagaUtils.createRecord(
                        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        leaderboardId,
                        comp
                );

        SagaUtils.addSagaHeaders(rec, sagaId, SagaUtils.newMessageId(), leaderboardId);
        kafkaTemplate.send(rec);

        log.info("Sent delete leaderboard compensation for {} (saga {})", leaderboardId, sagaId);
    }
}