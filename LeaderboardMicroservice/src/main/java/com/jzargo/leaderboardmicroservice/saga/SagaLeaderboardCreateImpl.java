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
                .status(SagaStep.LEADERBOARD_CREATE)
                .lastStepCompleted(SagaStep.INITIATED.name())
                .build();

        sagaControllingStateRepository.save(build);

        ProducerRecord<String, Object> record =
                SagaUtils.createRecord(
                        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        region,
                        map);

        String messageId = SagaUtils.newMessageId();
        SagaUtils.addSagaHeaders(record, build.getId(), messageId, region);

        kafkaTemplate.send(record);
    }

    @Override
    @Transactional
    public boolean stepCreateLeaderboard(InitLeaderboardCreateEvent event, String region, String sagaId) {
        if (event.isMutable() && (event.getEvents() == null || event.getEvents().isEmpty())) {
            throw new IllegalArgumentException("Mutable leaderboard must have at least one event");
        }

        SagaControllingState sagaControllingState = sagaControllingStateRepository.findById(sagaId).orElseThrow();
        log.info("stepCreateLeaderboard: event.isMutable() = {}", event.isMutable());
        if (sagaControllingState.getStatus() != SagaStep.LEADERBOARD_CREATE){
            log.warn("Saga cannot process event");
            return false;
        }

        String leaderboardId = leaderboardService.createLeaderboard(event, region);
        event.setLbId(leaderboardId);


        sagaControllingState.setLeaderboardId(leaderboardId);
        sagaControllingState.setStatus(SagaStep.OPTIONAL_EVENTS_CREATE);
        sagaControllingState.setLastStepCompleted(SagaStep.LEADERBOARD_CREATE.name());
        sagaControllingStateRepository.save(sagaControllingState);

        if (event.isMutable()) {
            sentEventCreate(sagaId, event);
            return true;
        } else {
            try {
                sentToUserProfile(sagaId, event.getLbId(), event.getNameLb(), event.getOwnerId());
                return true;
            } catch (Exception e) {
                log.error("Error executing saga lua script for saga {}", sagaId, e);
            }
            return false;
        }
    }


    @Override
    public void stepSuccessfulEventInit(SuccessfulEventInitialization successfulEventInitialization, String sagaId) {
        LeaderboardInfo leaderboardInfo = leaderboardInfoRepository
                .findById(successfulEventInitialization.getLbId())
                .orElseThrow();

        sentToUserProfile(sagaId, successfulEventInitialization.getLbId(), leaderboardInfo.getName(), leaderboardInfo.getOwnerId());
    }

    @Override
    @Transactional
    public void stepSagaCompleted(UserAddedLeaderboard userAddedLeaderboard, String sagaId) {
        stringRedisTemplate.execute(
                sagaSuccessfulScript, List.of("saga_controlling_state" + ":" + sagaId),
                userAddedLeaderboard.getLbId(), SagaStep.COMPLETE.name(),
                SagaStep.USER_PROFILE_UPDATE.name()
        );

        leaderboardService.confirmLbCreation(userAddedLeaderboard.getLbId());
    }

    @Transactional
    @Override
    public void compensateStepUserProfile(String sagaId, FailedLeaderboardCreation failedLeaderboardCreation) {
        SagaControllingState sagaState = sagaControllingStateRepository.findById(sagaId).orElseThrow();
        String leaderboardId = sagaState.getLeaderboardId();
        LeaderboardInfo leaderboardInfo = leaderboardInfoRepository.findById(leaderboardId).orElseThrow();

        if (!leaderboardInfo.isMutable()) {
            sagaState.setStatus(SagaStep.COMPENSATE_LEADERBOARD);
            sagaState.setLastStepCompleted(SagaStep.COMPENSATE_USER_PROFILE.name());

        } else {
            sagaState.setLastStepCompleted(sagaState.getStatus().name());
            sagaState.setStatus(SagaStep.COMPENSATE_USER_PROFILE);

            LeaderboardEventDeletion leaderboardEventDeletion =
                    new LeaderboardEventDeletion(failedLeaderboardCreation.getLbId());

            ProducerRecord<String, Object> record =
                    SagaUtils.createRecord(
                            KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                            sagaId,
                            leaderboardEventDeletion
                    );
            String s = SagaUtils.newMessageId();
            SagaUtils.addSagaHeaders(record, sagaId,s,sagaId);
            kafkaTemplate.send(record);
        }
        sagaControllingStateRepository.save(sagaState);

    }

    //Compensates caught event from event micros and means events are not created
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
            } else if (!sagaState.getStatus().equals(SagaStep.OPTIONAL_EVENTS_CREATE) ) {
                log.warn("Cannot compensate because of saga state");
                return;
            }

            log.info("Sent CompensateOptionalEventsCommand for saga {}", sagaId);
            sagaState.setStatus(SagaStep.COMPENSATE_LEADERBOARD);
            sagaState.setLastStepCompleted(SagaStep.COMPENSATE_OPTIONAL_EVENT.name());
            sagaControllingStateRepository.save(sagaState);

        } catch (Exception e) {
            log.error("Error during compensateStepOptionalEvent for saga {}", sagaId, e);
        }
    }

    //Compensates while creating was successful but there are external factors
    @Override
    public void compensateStepOptionalEvent(String sagaId, LeaderboardEventDeletion leaderboardEventDeletion) {
        try {
            SagaControllingState sagaState = sagaControllingStateRepository
                    .findById(sagaId)
                    .orElseThrow(() -> new IllegalArgumentException("Saga not found " + sagaId));

            String leaderboardId = sagaState.getLeaderboardId();
            if (leaderboardId == null) {
                log.warn("Cannot compensate mutable events — no leaderboardId for saga {}", sagaId);
                sagaState.setStatus(SagaStep.FAILED);
                sagaControllingStateRepository.save(sagaState);
                return;
            }

            log.info("Sent compensate for saga step leaderboard {}", sagaId);

            sagaState.setStatus(SagaStep.COMPENSATE_LEADERBOARD);
            sagaState.setLastStepCompleted(SagaStep.COMPENSATE_OPTIONAL_EVENT.name());
            sagaControllingStateRepository.save(sagaState);
            sentLeaderboardDeletion(sagaId, leaderboardEventDeletion.getLbId());
        } catch (Exception e) {
            log.error("Error during compensateStepOptionalEvent for saga {}", sagaId, e);
        }
    }

    @Override
    public void stepCompensateLeaderboard(DeleteLbEvent dle, String sagaId) {
        SagaControllingState sagaState = sagaControllingStateRepository.findById(sagaId)
                .orElseThrow();

        if (dle.getLbId() == null) {
            log.warn("Cannot compensate leaderboard step — no leaderboardId for saga {}", sagaId);
            sagaState.setStatus(SagaStep.FAILED);
            sagaControllingStateRepository.save(sagaState);
            return;
        } else if (sagaState.getStatus() != SagaStep.COMPENSATE_LEADERBOARD) {
            log.warn("cannot compensate because status equals {}", sagaState.getStatus());
            return;
        }
        leaderboardService.deleteLeaderboard(dle.getLbId(), sagaId);

    }

    @Override
    public boolean stepOutOfTime(String lbId) {
        List<SagaControllingState> sagas = sagaControllingStateRepository.findByLeaderboardId(lbId);
        if (sagas == null) {
            log.error("No sagas found for lbId {}", lbId);
            return true;
        }
        if (sagas.size() != 1) {
            log.error("Multiple sagas found for lbId {}", lbId);
            sagas.forEach(saga -> {
                saga.setStatus(SagaStep.FAILED);
                sagaControllingStateRepository.save(saga);
            });
            return true;
        }

        SagaControllingState saga = sagas.getFirst();

        switch (saga.getStatus()) {
            case LEADERBOARD_CREATE -> stepCompensateLeaderboard(
                    new DeleteLbEvent(lbId), saga.getId()
            );
            case OPTIONAL_EVENTS_CREATE -> compensateStepOptionalEvent(
                    saga.getId(), new LeaderboardEventDeletion(lbId));
            case USER_PROFILE_UPDATE -> {} //TODO: realize sending to user profile event due to the fact that step which compensate for user profile does not exist
            case COMPLETE -> {
                log.warn("Saga was completed but in some reasons it does not confirmed");
                leaderboardService.confirmLbCreation(lbId);
                return false;
            }
            case FAILED -> leaderboardService.deleteLeaderboard(lbId, saga.getId());
            case null, default -> log.warn("Saga either does not have status or it has already compensated");

        }
        return true;
    }

    private void sentLeaderboardDeletion(String sagaId,String lbId){
        ProducerRecord<String, Object> record =
                SagaUtils.createRecord(
                        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        sagaId,
                        new DeleteLbEvent(lbId)
                );
        String s = SagaUtils.newMessageId();
        SagaUtils.addSagaHeaders(record, sagaId,s,sagaId);
        kafkaTemplate.send(record);
    }
    private void sentToUserProfile(String sagaId, String leaderboardId, String nameLb, long ownerId) {
        stringRedisTemplate.execute(
                sagaSuccessfulScript,
                List.of("saga_controlling_state" + ":" + sagaId),
                leaderboardId, SagaStep.USER_PROFILE_UPDATE.name(), SagaStep.OPTIONAL_EVENTS_CREATE.name()
        );

        UserNewLeaderboardCreated userNewLeaderboardCreated = new UserNewLeaderboardCreated(leaderboardId, nameLb, ownerId);

        ProducerRecord<String, Object> record =
                SagaUtils.createRecord(
                        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        sagaId,
                        userNewLeaderboardCreated);

        String messageId = SagaUtils.newMessageId();
        SagaUtils.addSagaHeaders(record, sagaId, messageId, sagaId);
        kafkaTemplate.send(record);

    }
    private void sentEventCreate(String sagaId, InitLeaderboardCreateEvent event) {
        try {
            stringRedisTemplate.execute(
                    sagaSuccessfulScript,
                    List.of("saga_controlling_state" + ":" + sagaId),
                    event.getLbId(),
                    SagaStep.OPTIONAL_EVENTS_CREATE.name(),
                    SagaStep.LEADERBOARD_CREATE.name()
            );
        } catch (Exception e) {
            log.error("Error executing saga lua script for saga {}", sagaId, e);
        }

        LeaderboardEventInitialization build = LeaderboardEventInitialization.builder()
                .lbId(event.getLbId())
                .events(event.getEvents())
                .userId(event.getOwnerId())
                .isPublic(event.isPublic())
                .metadata(Map.of("expire_date", event.getExpireAt()))
                .build();

        ProducerRecord<String, Object> record =
                SagaUtils.createRecord(
                        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        sagaId,
                        build);

        String messageId = SagaUtils.newMessageId();
        SagaUtils.addSagaHeaders(record, sagaId, messageId, sagaId);

        kafkaTemplate.send(record);

    }

}