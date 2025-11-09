package com.jzargo.leaderboardmicroservice.saga;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.entity.SagaStep;
import com.jzargo.leaderboardmicroservice.mapper.CreateInitialCreateLeaderboardSagaRequestMapper;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.sagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class SagaLeaderboardCreateImpl implements SagaLeaderboardCreate{

    private final CreateInitialCreateLeaderboardSagaRequestMapper createInitialCreateLeaderboardSagaRequestMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final sagaControllingStateRepository sagaControllingStateRepository;
    private final LeaderboardService leaderboardService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<String> sagaSuccessfulScript;
    private final LeaderboardInfoRepository leaderboardInfoRepository;

    public SagaLeaderboardCreateImpl(
            CreateInitialCreateLeaderboardSagaRequestMapper createInitialCreateLeaderboardSagaRequestMapper,
            KafkaTemplate<String, Object> kafkaTemplate,
            sagaControllingStateRepository sagaControllingStateRepository, LeaderboardService leaderboardService,
            StringRedisTemplate stringRedisTemplate, RedisScript<String> sagaSuccessfulScript, LeaderboardInfoRepository leaderboardInfoRepository){
        this.createInitialCreateLeaderboardSagaRequestMapper = createInitialCreateLeaderboardSagaRequestMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.sagaControllingStateRepository = sagaControllingStateRepository;
        this.leaderboardService = leaderboardService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sagaSuccessfulScript = sagaSuccessfulScript;
        this.leaderboardInfoRepository = leaderboardInfoRepository;
    }

    @Override
    public void startSaga(CreateLeaderboardRequest request, long userId, String username, String region) {
        InitLeaderboardCreateEvent map = createInitialCreateLeaderboardSagaRequestMapper.map(request);
        map.setOwnerId(userId);
        map.setUsername(username);
        SagaControllingState build = SagaControllingState.builder()
                .id(UUID.randomUUID().toString())
                .status(SagaStep.INITIATED)
                .lastStepCompleted("")
                .build();

        sagaControllingStateRepository.save(build);

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC, region, map);

        record.headers()
                .add(KafkaConfig.MESSAGE_ID, UUID.randomUUID().toString().getBytes())
                .add(KafkaConfig.SAGA_ID_HEADER, build.getId().getBytes())
                .add(KafkaHeaders.RECEIVED_KEY, region.getBytes());
        kafkaTemplate.send(record);
    }

    @Override
    public boolean stepCreateLeaderboard(InitLeaderboardCreateEvent event, String region, String sagaId){
        if (event.isMutable() && event.getEvents().isEmpty()) {
            throw new IllegalArgumentException("Cannot events in mutable board be less than 1");
        }

        String leaderboardId = leaderboardService.createLeaderboard(event, region);

        SagaControllingState sagaControllingState = sagaControllingStateRepository
                .findById(sagaId)
                .orElseThrow();

        sagaControllingState.setLeaderboardId(leaderboardId);
        sagaControllingStateRepository.save(sagaControllingState);

        ProducerRecord<String, Object> record;
        if (event.isMutable()) {
            stringRedisTemplate.execute(
                    sagaSuccessfulScript, List.of("saga_controlling_state" + ":" + sagaId),
                    leaderboardId, SagaStep.LEADERBOARD_CREATED, SagaStep.INITIATED, ""
            );

            LeaderboardEventInitialization build =
                    LeaderboardEventInitialization.builder()
                            .lbId(leaderboardId)
                            .events(event.getEvents())
                            .userId(event.getOwnerId())
                            .isPublic(event.isPublic())
                            .metadata(
                                    Map.of("expire_date", event.getExpireAt())
                            )
                            .build();
            record = new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                    leaderboardId, build);
            return true;
        } else {
            stringRedisTemplate.execute(
                    sagaSuccessfulScript, List.of("saga_controlling_state" + ":" + sagaId),
                    leaderboardId, SagaStep.OPTIONAL_EVENTS_CREATED, SagaStep.INITIATED, ""
            );

            return false;
        }
    }

    @Override
    public void stepSuccessfulEventInit(
            SuccessfulEventInitialization successfulEventInitialization,
            String sagaId) {

        SagaControllingState sagaControllingState = sagaControllingStateRepository
                .findById(sagaId)
                .orElseThrow();

        sagaControllingState.setStatus(SagaStep.USER_PROFILE_UPDATE);
        sagaControllingState.setLastStepCompleted(SagaStep.OPTIONAL_EVENTS_CREATED.name());

        sagaControllingStateRepository.save(sagaControllingState);
    }

    @Override
    @Transactional
    public void stepSagaCompleted(
            UserAddedLeaderboard userAddedLeaderboard,
            String sagaId
    ) {
        stringRedisTemplate.execute(
                sagaSuccessfulScript, List.of("saga_controlling_state" + ":" + sagaId),
                userAddedLeaderboard.getLbId(), SagaStep.COMPLETED,
                SagaStep.OPTIONAL_EVENTS_CREATED, SagaStep.OPTIONAL_EVENTS_CREATED
        );
        LeaderboardInfo leaderboardInfo = leaderboardInfoRepository.findById(
                userAddedLeaderboard.getLbId()
        ).orElseThrow();
        stringRedisTemplate.persist(leaderboardInfo.getKey());

    }

    @Transactional
    @Override
    public void handleFailedCreation(
            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
            @Header(KafkaConfig.MESSAGE_ID) String messageId,
            @Payload FailedLeaderboardCreation failedLeaderboardCreation
            ) {
        SagaControllingState sagaState = sagaControllingStateRepository
                    .findById(sagaId)
                    .orElseThrow();
            sagaState.setStatus(SagaStep.FAILED);

            String leaderboardId = sagaState.getLeaderboardId();
            LeaderboardInfo leaderboardInfo = leaderboardInfoRepository.findById(leaderboardId).orElseThrow();

            ReverseUserAdding reverseUserAdding = new ReverseUserAdding(
                    failedLeaderboardCreation.getUserId(),
                    failedLeaderboardCreation.getLbId());

            ProducerRecord<String, Object> record = new ProducerRecord<>(
                    KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                    sagaId,
                    reverseUserAdding);

            record.headers()
                    .add(KafkaConfig.MESSAGE_ID, UUID.randomUUID().toString().getBytes())
                    .add(KafkaConfig.SAGA_ID_HEADER, sagaId.getBytes())
                    .add(KafkaHeaders.RECEIVED_KEY, sagaId.getBytes());

            kafkaTemplate.send(record);
        }
    }
}