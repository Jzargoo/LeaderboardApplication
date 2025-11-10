package com.jzargo.leaderboardmicroservice.saga;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.entity.SagaStep;
import com.jzargo.leaderboardmicroservice.mapper.CreateInitialCreateLeaderboardSagaRequestMapper;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
@Slf4j
public class SagaLeaderboardCreateImpl implements SagaLeaderboardCreate {

    private final CreateInitialCreateLeaderboardSagaRequestMapper createInitialCreateLeaderboardSagaRequestMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SagaControllingStateRepository sagaControllingStateRepository;
    private final LeaderboardService leaderboardService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<String> sagaSuccessfulScript;
    private final LeaderboardInfoRepository leaderboardInfoRepository;

    public SagaLeaderboardCreateImpl(
            CreateInitialCreateLeaderboardSagaRequestMapper createInitialCreateLeaderboardSagaRequestMapper,
            KafkaTemplate<String, Object> kafkaTemplate,
            SagaControllingStateRepository sagaControllingStateRepository,
            LeaderboardService leaderboardService,
            StringRedisTemplate stringRedisTemplate,
            RedisScript<String> sagaSuccessfulScript,
            LeaderboardInfoRepository leaderboardInfoRepository) {
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

        ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC, region, map);

        record.headers()
                .add(KafkaConfig.MESSAGE_ID, SagaUtils.newMessageId().getBytes())
                .add(KafkaConfig.SAGA_ID_HEADER, build.getId().getBytes())
                .add(KafkaHeaders.RECEIVED_KEY, region.getBytes());

        kafkaTemplate.send(record);
    }

    @Override
    public boolean stepCreateLeaderboard(InitLeaderboardCreateEvent event, String region, String sagaId) {
        if (event.isMutable() && (event.getEvents() == null || event.getEvents().isEmpty())) {
            throw new IllegalArgumentException("Mutable leaderboard must have at least one event");
        }

        String leaderboardId = leaderboardService.createLeaderboard(event, region);

        SagaControllingState sagaControllingState = sagaControllingStateRepository.findById(sagaId).orElseThrow();

        sagaControllingState.setLeaderboardId(leaderboardId);
        sagaControllingStateRepository.save(sagaControllingState);

        if (event.isMutable()) {
            try {
                stringRedisTemplate.execute(
                        sagaSuccessfulScript,
                        List.of("saga_controlling_state" + ":" + sagaId),
                        leaderboardId, SagaStep.LEADERBOARD_CREATED.name(), SagaStep.INITIATED.name(), ""
                );
            } catch (Exception e) {
                log.error("Error executing saga lua script for saga {}", sagaId, e);
            }

            LeaderboardEventInitialization build = LeaderboardEventInitialization.builder()
                    .lbId(leaderboardId)
                    .events(event.getEvents())
                    .userId(event.getOwnerId())
                    .isPublic(event.isPublic())
                    .metadata(Map.of("expire_date", event.getExpireAt()))
                    .build();

            ProducerRecord<String, Object> record = new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                    leaderboardId, build);
            record.headers()
                    .add(KafkaConfig.MESSAGE_ID, SagaUtils.newMessageId().getBytes())
                    .add(KafkaConfig.SAGA_ID_HEADER, sagaId.getBytes())
                    .add(KafkaHeaders.RECEIVED_KEY, leaderboardId.getBytes());

            kafkaTemplate.send(record);
            return true;
        } else {
            try {
                stringRedisTemplate.execute(
                        sagaSuccessfulScript,
                        List.of("saga_controlling_state" + ":" + sagaId),
                        leaderboardId, SagaStep.OPTIONAL_EVENTS_CREATED.name(), SagaStep.INITIATED.name(), ""
                );
            } catch (Exception e) {
                log.error("Error executing saga lua script for saga {}", sagaId, e);
            }
            return false;
        }
    }

    @Override
    public void stepSuccessfulEventInit(SuccessfulEventInitialization successfulEventInitialization, String sagaId) {
        SagaControllingState sagaControllingState = sagaControllingStateRepository.findById(sagaId).orElseThrow();

        sagaControllingState.setStatus(SagaStep.USER_PROFILE_UPDATE);
        sagaControllingState.setLastStepCompleted(SagaStep.OPTIONAL_EVENTS_CREATED.name());

        sagaControllingStateRepository.save(sagaControllingState);
    }

    @Override
    @Transactional
    public void stepSagaCompleted(UserAddedLeaderboard userAddedLeaderboard, String sagaId) {
        LeaderboardInfo leaderboardInfo = leaderboardInfoRepository.findById(userAddedLeaderboard.getLbId())
                .orElseThrow(() -> new IllegalArgumentException("Leaderboard not found: " + userAddedLeaderboard.getLbId()));

        stringRedisTemplate.execute(
                sagaSuccessfulScript, List.of("saga_controlling_state" + ":" + sagaId),
                userAddedLeaderboard.getLbId(), SagaStep.COMPLETED.name(),
                SagaStep.OPTIONAL_EVENTS_CREATED.name(), SagaStep.OPTIONAL_EVENTS_CREATED.name()
        );

        stringRedisTemplate.persist(leaderboardInfo.getKey());
        leaderboardInfo.setActive(true);
        leaderboardInfoRepository.save(leaderboardInfo);
    }

    @Transactional
    @Override
    public void compensateStepUserProfile(String sagaId, FailedLeaderboardCreation failedLeaderboardCreation) {
        SagaControllingState sagaState = sagaControllingStateRepository.findById(sagaId).orElseThrow();
        String leaderboardId = sagaState.getLeaderboardId();
        LeaderboardInfo leaderboardInfo = leaderboardInfoRepository.findById(leaderboardId).orElseThrow();

        if (!leaderboardInfo.isMutable()) {
            sagaState.setStatus(SagaStep.FAILED);
            sagaState.setLastStepCompleted(SagaStep.COMPENSATE_USER_PROFILE.name());
        } else {
            sagaState.setLastStepCompleted(sagaState.getStatus().name());
            sagaState.setStatus(SagaStep.COMPENSATE_USER_PROFILE);

            CompensateOptionalEventsCommand compensateOptionalEventsCommand =
                    new CompensateOptionalEventsCommand(sagaId, failedLeaderboardCreation.getLbId());

            ProducerRecord<String, Object> record =
                    SagaUtils.createRecord(
                            KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                            sagaId,
                            compensateOptionalEventsCommand
                    );
            String s = SagaUtils.newMessageId();
            SagaUtils.addSagaHeaders(record, sagaId,s,sagaId);
            kafkaTemplate.send(record);
        }
        sagaControllingStateRepository.save(sagaState);

    }

    @Transactional
    @Override
    public void compensateStepOptionalEvent(String sagaId, FailedLeaderboardCreation failedLeaderboardCreation) {
        try {
            SagaControllingState sagaState = sagaControllingStateRepository
                    .findById(sagaId)
                    .orElseThrow(() -> new IllegalArgumentException("Saga not found " + sagaId));

            String leaderboardId = sagaState.getLeaderboardId();
            if (leaderboardId == null) {
                log.warn("Cannot compensate optional events — no leaderboardId for saga {}", sagaId);
                sagaState.setStatus(SagaStep.FAILED);
                sagaControllingStateRepository.save(sagaState);
                return;
            }

            CompensateOptionalEventsCommand cmd = new CompensateOptionalEventsCommand(
                    sagaId,
                    leaderboardId
            );

            ProducerRecord<String, Object> record =
                    new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                            leaderboardId,
                            cmd);

            SagaUtils.addSagaHeaders(record, sagaId, SagaUtils.newMessageId(), leaderboardId);

            kafkaTemplate.send(record);

            log.info("Sent CompensateOptionalEventsCommand for saga {}", sagaId);

            sagaState.setStatus(SagaStep.FAILED);
            sagaState.setLastStepCompleted(SagaStep.OPTIONAL_EVENTS_CREATED.name());
            sagaControllingStateRepository.save(sagaState);

        } catch (Exception e) {
            log.error("Error during compensateStepOptionalEvent for saga {}", sagaId, e);
        }
    }
}