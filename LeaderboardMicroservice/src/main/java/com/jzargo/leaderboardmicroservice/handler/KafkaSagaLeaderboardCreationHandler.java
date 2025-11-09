package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.sagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.SuccessfulEventInitialization;
import com.jzargo.messaging.UserAddedLeaderboard;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@KafkaListener(topics = {
        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
        KafkaConfig.GROUP_ID
})
@Component
@Slf4j
public class KafkaSagaLeaderboardCreationHandler {
    private final StringRedisTemplate stringRedisTemplate;
    private final SagaLeaderboardCreate sagaLeaderboardCreate;
    private final sagaControllingStateRepository sagaControllingStateRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final LeaderboardInfoRepository leaderboardInfoRepository;

    public KafkaSagaLeaderboardCreationHandler(StringRedisTemplate stringRedisTemplate, SagaLeaderboardCreate sagaLeaderboardCreate, sagaControllingStateRepository sagaControllingStateRepository, KafkaTemplate<String, Object> kafkaTemplate, LeaderboardInfoRepository leaderboardInfoRepository) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.sagaLeaderboardCreate = sagaLeaderboardCreate;
        this.sagaControllingStateRepository = sagaControllingStateRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.leaderboardInfoRepository = leaderboardInfoRepository;
    }

    @KafkaHandler
    @Transactional
    public void handleCreateLeaderboardSaga(InitLeaderboardCreateEvent event,
                                            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
                                            @Header(KafkaConfig.MESSAGE_ID) String messageId,
                                            @Header(KafkaHeaders.RECEIVED_KEY) String region
    ){
        String key = "processed:" + messageId;
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(7));
        if(success != null && !success) {
            getWarn(messageId);
            return;
        }
        try {
            SagaControllingState sagaControllingState = sagaControllingStateRepository
                    .findById(sagaId)
                    .orElseThrow();

            boolean b = sagaLeaderboardCreate.stepCreateLeaderboard(event, region, sagaId);

            ProducerRecord<String, Object> record;
            if(b) {
                LeaderboardEventInitialization build =
                        LeaderboardEventInitialization.builder()
                                .lbId(sagaControllingState
                                        .getLeaderboardId())
                                .events(event.getEvents())
                                .userId(event.getOwnerId())
                                .isPublic(event.isPublic())
                                .metadata(
                                        Map.of("expire_date", event.getExpireAt())
                                )
                                .build();
                record = new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        sagaControllingState.getLeaderboardId(),
                        build);

            } else {
                UserNewLeaderboardCreated userNewLeaderboardCreated = new UserNewLeaderboardCreated(
                        sagaControllingState.getLeaderboardId(),
                        event.getNameLb(),
                        event.getOwnerId()
                );

                record = new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        sagaControllingState.getLeaderboardId(),
                        userNewLeaderboardCreated);
            }

            record.headers()
                    .add(KafkaConfig.MESSAGE_ID, UUID.randomUUID().toString().getBytes())
                    .add(KafkaConfig.SAGA_ID_HEADER, sagaId.getBytes())
                    .add(KafkaHeaders.RECEIVED_KEY, sagaId.getBytes());

            kafkaTemplate.send(record);

            log.info("Processed saga create leaderboard with id {}", sagaId);

        }catch (Exception e){
            log.error("Error while processed message {}", messageId,e);
            stringRedisTemplate.delete(key);
        }
    }

    @KafkaHandler
    @Transactional
    public void handleSuccessfulEventInitialization(
            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
            @Header(KafkaConfig.MESSAGE_ID) String messageId,
            @Payload SuccessfulEventInitialization successfulEventInitialization
    ) {
        String key = "processed:" + messageId;
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(7));
        if(success != null && !success) {
            getWarn(messageId);
            return;
        }
        try{
            sagaLeaderboardCreate.stepSuccessfulEventInit(successfulEventInitialization, sagaId);
            LeaderboardInfo byId = leaderboardInfoRepository.findById(successfulEventInitialization.getLbId())
                    .orElseThrow(()-> new IllegalArgumentException("lb id cannot be a null"));

            UserNewLeaderboardCreated userNewLeaderboardCreated = new UserNewLeaderboardCreated(
                    successfulEventInitialization.getLbId(), byId.getName(),
                    byId.getOwnerId()
            );
            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                    byId.getId(), userNewLeaderboardCreated);

            record.headers()
                    .add(KafkaConfig.MESSAGE_ID, UUID.randomUUID().toString().getBytes())
                    .add(KafkaConfig.SAGA_ID_HEADER, sagaId.getBytes())
                    .add(KafkaHeaders.RECEIVED_KEY, sagaId.getBytes());

            kafkaTemplate.send(record);

        } catch (IllegalArgumentException e) {
            log.error("Incorrect event without lb id");
        }
        catch (Exception e) {
            log.error("Error while processed message {}", messageId,e);
            stringRedisTemplate.delete(key);
        }
    }

    public void handleUserAddedLeaderboard(
            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
            @Header(KafkaConfig.MESSAGE_ID) String messageId,
            @Payload UserAddedLeaderboard userAddedLeaderboard
    ) {
        String key = "processed:" + messageId;
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(7));

        if(success != null && !success) {
            getWarn(messageId);
            return;
        }
        try {
            sagaLeaderboardCreate.stepSagaCompleted(userAddedLeaderboard, sagaId);


        } catch (Exception e) {
            stringRedisTemplate.delete(key);
        }

    }
        private static void getWarn(String messageId) {
        log.warn("Handled processed message with id {}", messageId);
    }
}
