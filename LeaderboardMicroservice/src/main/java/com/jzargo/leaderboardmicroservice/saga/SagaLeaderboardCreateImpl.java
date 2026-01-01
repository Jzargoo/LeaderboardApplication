package com.jzargo.leaderboardmicroservice.saga;

import com.jzargo.leaderboardmicroservice.client.*;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.entity.SagaStep;
import com.jzargo.leaderboardmicroservice.mapper.CreateInitialCreateLeaderboardSagaRequestMapper;
import com.jzargo.leaderboardmicroservice.mapper.CreateLeaderboardEventInitializationCreateMapper;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
public class SagaLeaderboardCreateImpl implements SagaLeaderboardCreate {

    private final CreateInitialCreateLeaderboardSagaRequestMapper mapper;
    private final LeaderboardServiceWebProxy leaderboardServiceWebProxy;
    private final ScoringServiceWebProxy scoringServiceWebProxy;
    private final UserServiceWebProxy userServiceWebProxy;
    private final SagaControllingStateRepository sagaRepository;
    private final LeaderboardService leaderboardService;
    private final LeaderboardInfoRepository leaderboardInfoRepository;
    private final CreateLeaderboardEventInitializationCreateMapper createLeaderboardEventInitializationCreateMapper;

    @Value("${proxy.mode.leaderboard:kafka}")
    private String leaderboardProxyMode;
    @Value("${proxy.mode.user:kafka}")
    private String userProxyMode;
    @Value("${proxy.mode.scoring:kafka}")
    private String scoringProxyMode;

    public SagaLeaderboardCreateImpl(
            CreateInitialCreateLeaderboardSagaRequestMapper mapper,
            SagaControllingStateRepository sagaRepository,
            LeaderboardService leaderboardService,
            LeaderboardInfoRepository leaderboardInfoRepository,
            FactoryLeaderboardWebProxy leaderboardProxyFactory,
            FactoryUserWebProxy userProxyFactory,
            FactoryScoringWebProxy scoringProxyFactory,
            CreateLeaderboardEventInitializationCreateMapper createLeaderboardEventInitializationCreateMapper) {
        this.mapper = mapper;
        this.sagaRepository = sagaRepository;
        this.leaderboardService = leaderboardService;
        this.leaderboardInfoRepository = leaderboardInfoRepository;

        this.leaderboardServiceWebProxy =
                leaderboardProxyFactory.getClient(leaderboardProxyMode);
        this.userServiceWebProxy =
                userProxyFactory.getClient(userProxyMode);
        this.scoringServiceWebProxy =
                scoringProxyFactory.getClient(scoringProxyMode);
        this.createLeaderboardEventInitializationCreateMapper = createLeaderboardEventInitializationCreateMapper;
    }

    @Override
    public void startSaga(CreateLeaderboardRequest request,
                          long userId,
                          String username,
                          String region) {

        InitLeaderboardCreateEvent event = mapper.map(request);
        event.setOwnerId(userId);
        event.setUsername(username);

        SagaControllingState saga = SagaControllingState.builder()
                .id(UUID.randomUUID().toString())
                .status(SagaStep.LEADERBOARD_CREATE)
                .lastStepCompleted(SagaStep.INITIATED.name())
                .build();

        sagaRepository.save(saga);
        leaderboardServiceWebProxy.createLeaderboard(event, saga.getId());
    }

    @Override
    @Transactional
    public boolean stepCreateLeaderboard(InitLeaderboardCreateEvent event, String sagaId) {

        SagaControllingState saga = sagaRepository.findById(sagaId).orElseThrow();

        if (saga.getStatus() != SagaStep.LEADERBOARD_CREATE &&
            ! SagaStep.INITIATED .name().equals(
                    saga.getLastStepCompleted()
            )
        ) {
            log.warn("Saga {} is in invalid state {}", sagaId, saga.getStatus());
            return false;
        }

        if (event.isMutable() && (event.getEvents() == null || event.getEvents().isEmpty())) {
            throw new IllegalArgumentException("Mutable leaderboard must contain events");
        }

        String leaderboardId = leaderboardService.createLeaderboard(event);

        saga.setLeaderboardId(leaderboardId);
        saga.setLastStepCompleted(SagaStep.LEADERBOARD_CREATE.name());

        if (event.isMutable()) {
            saga.setStatus(SagaStep.OPTIONAL_EVENTS_CREATE);
            sagaRepository.save(saga);

            scoringServiceWebProxy.initiateEvents(
                    createLeaderboardEventInitializationCreateMapper
                            .map(event)
                    ,
                    sagaId
            );
            return true;
        }

        saga.setStatus(SagaStep.USER_PROFILE_UPDATE);
        sagaRepository.save(saga);

        userServiceWebProxy.createUserAddedLeaderboard(
                new UserNewLeaderboardCreated(leaderboardId, event.getNameLb(), event.getOwnerId()),
                sagaId
        );
        return true;
    }

    @Override
    public void stepSuccessfulEventInit(SuccessfulEventInitialization event, String sagaId) {

        SagaControllingState saga = sagaRepository.findById(sagaId).orElseThrow();

        if (saga.getStatus() != SagaStep.OPTIONAL_EVENTS_CREATE) {
            log.warn("Ignoring SuccessfulEventInitialization for saga {}", sagaId);
            return;
        }

        saga.setLastStepCompleted(SagaStep.OPTIONAL_EVENTS_CREATE.name());
        saga.setStatus(SagaStep.USER_PROFILE_UPDATE);
        sagaRepository.save(saga);

        LeaderboardInfo leaderboard = leaderboardInfoRepository
                .findById(event.getLbId())
                .orElseThrow();

        userServiceWebProxy.createUserAddedLeaderboard(
                new UserNewLeaderboardCreated(
                        leaderboard.getId(),
                        leaderboard.getName(),
                        leaderboard.getOwnerId()),
                sagaId
        );

    }

    @Override
    @Transactional
    public void stepSagaCompleted(UserAddedLeaderboard event, String sagaId) {

        SagaControllingState saga = sagaRepository.findById(sagaId).orElseThrow();

        if (saga.getStatus() != SagaStep.COMPLETE &&
            ! SagaStep.USER_PROFILE_UPDATE.name().equals(
                    saga.getLastStepCompleted()
            )
        ) {

            log.warn("Saga {} cannot be completed from {}", sagaId, saga.getStatus());
            return;

        }

        saga.setLastStepCompleted(SagaStep.USER_PROFILE_UPDATE.name());
        saga.setStatus(SagaStep.COMPLETE);
        sagaRepository.save(saga);

        leaderboardService.confirmLbCreation(event.getLbId());
    }

    @Transactional
    @Override
    public void compensateStepUserProfile(String sagaId, FailedLeaderboardCreation failed) {

        SagaControllingState saga = sagaRepository.findById(sagaId).orElseThrow();

        if (saga.getStatus() != SagaStep.USER_PROFILE_UPDATE &&
                !SagaStep.OPTIONAL_EVENTS_CREATE
                        .name().equals(
                        saga.getLastStepCompleted()
                )) {
            return;
        }

        saga.setLastStepCompleted(SagaStep.USER_PROFILE_UPDATE.name());
        saga.setStatus(SagaStep.COMPENSATE_USER_PROFILE);
        sagaRepository.save(saga);

        scoringServiceWebProxy.deleteLeaderboardEvents(
                new LeaderboardEventDeletion(failed.getLbId()), sagaId
        );
    }

    @Transactional
    @Override
    public void compensateStepOptionalEvent(String sagaId, String lbId) {

        SagaControllingState saga = sagaRepository.findById(sagaId).orElseThrow();

        if (saga.getStatus() != SagaStep.OPTIONAL_EVENTS_CREATE &&
            ! SagaStep.LEADERBOARD_CREATE
                    .name().equals (
                        saga.getLastStepCompleted()
            )
        ) {
            return;
        }

        saga.setLastStepCompleted(SagaStep.COMPENSATE_OPTIONAL_EVENT.name());
        saga.setStatus(SagaStep.COMPENSATE_LEADERBOARD);

        leaderboardServiceWebProxy.compensateLeaderboard(
                new DeleteLbEvent(lbId),
                sagaId
        );

        sagaRepository.save(saga);
    }


    @Override
    public void stepCompensateLeaderboard(DeleteLbEvent event, String sagaId) {

        SagaControllingState saga = sagaRepository.findById(sagaId).orElseThrow();

        if (saga.getStatus() != SagaStep.COMPENSATE_LEADERBOARD &&
            !SagaStep.COMPENSATE_OPTIONAL_EVENT.name()
                    .equals(
                            saga.getLastStepCompleted()
                    )
        ) {
            return;
        }

        leaderboardService.deleteLeaderboard(event.getLbId(), sagaId);
        saga.setStatus(SagaStep.FAILED);
        sagaRepository.save(saga);
    }

    @Override
    public boolean stepOutOfTime(String lbId) {
        List<SagaControllingState> sagas = sagaRepository.findByLeaderboardId(lbId);
        if (sagas == null) {
            log.error("No sagas found for lbId {}", lbId);
            return false;
        }
        if (sagas.size() != 1) {
            log.error("Multiple sagas found for lbId {}", lbId);
            sagas.forEach(saga -> {
                saga.setStatus(SagaStep.FAILED);
                sagaRepository.save(saga);
            });
            return false;
        }

        LeaderboardInfo leaderboardInfo = leaderboardInfoRepository
                .findById(lbId)
                .orElseThrow();

        SagaControllingState saga = sagas.getFirst();

        leaderboardServiceWebProxy.outOfTime(
                new OutOfTimeEvent(lbId, leaderboardInfo.getOwnerId()),
                saga.getId());

        leaderboardService.deleteLeaderboard(lbId, saga.getId());
        saga.setStatus(SagaStep.DELETED);
        sagaRepository.save(saga);

        return true;
    }
}