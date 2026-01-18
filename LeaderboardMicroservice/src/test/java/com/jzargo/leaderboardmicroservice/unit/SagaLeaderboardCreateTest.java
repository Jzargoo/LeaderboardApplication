package com.jzargo.leaderboardmicroservice.unit;

import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.entity.SagaStep;
import com.jzargo.leaderboardmicroservice.mapper.CreateInitialCreateLeaderboardSagaRequestMapper;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreateImpl;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.DeleteLbEvent;
import com.jzargo.messaging.FailedLeaderboardCreation;
import com.jzargo.messaging.UserAddedLeaderboard;
import com.jzargo.region.Regions;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SagaLeaderboardCreateTest {

    @Mock private CreateInitialCreateLeaderboardSagaRequestMapper mapper;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private SagaControllingStateRepository sagaRepository;
    @Mock private LeaderboardService leaderboardService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private RedisScript<String> sagaSuccessfulScript;
    @Mock private LeaderboardInfoRepository leaderboardInfoRepository;

    @InjectMocks private SagaLeaderboardCreateImpl sagaService;

    private CreateLeaderboardRequest request;
    private InitLeaderboardCreateEvent event;
    private SagaControllingState sagaState;
    private LeaderboardInfo leaderboardInfo;

    @BeforeEach
    void setup() {
        // Request DTO с более разнообразными данными
        request = new CreateLeaderboardRequest();
        request.setDescription("Test leaderboard for JUnit");
        request.setGlobalRange(15);
        request.setName("Leaderboard-Test");
        request.setOwnerId(42L);
        request.setInitialValue(150);
        request.setPublic(true);
        request.setMutable(false);
        request.setExpireAt(LocalDateTime.now().plusDays(3));
        request.setMaxScore(5000);
        request.setRegions(Set.of(Regions.GLOBAL.getCode()));
        request.setMaxEventsPerUser(10);
        request.setMaxEventsPerUserPerDay(3);
        request.setShowTies(true);
        request.setEvents(Map.of("KillEvent", 50.0, "WinEvent", 100.0));


        // Event DTO
        event = new InitLeaderboardCreateEvent();
        event.setOwnerId(request.getOwnerId());
        event.setUsername("testUser");
        event.setNameLb(request.getName());
        event.setMutable(request.isMutable());
        event.setEvents(request.getEvents());
        event.setPublic(request.isPublic());
        event.setRegions(request.getRegions());

        // Saga state
        sagaState = SagaControllingState.builder()
                .id(UUID.randomUUID().toString())
                .status(SagaStep.LEADERBOARD_CREATE)
                .build();

        // LeaderboardInfo
        leaderboardInfo = LeaderboardInfo.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .ownerId(request.getOwnerId())
                .isMutable(request.isMutable())
                .regions(Regions.GLOBAL.getCode())
                .build();
    }

    @Test
    void testStartSaga() {
        when(mapper.map(request)).thenReturn(event);

        sagaService.startSaga(request, request.getOwnerId(), event.getUsername());

        verify(sagaRepository).save(any(SagaControllingState.class));
        verify(kafkaTemplate).send(any(ProducerRecord.class));
        assertEquals(request.getOwnerId(), event.getOwnerId());
    }

    @Test
    void testStepCreateLeaderboard_NonMutable() {
        when(sagaRepository.findById(sagaState.getId())).thenReturn(Optional.of(sagaState));
        when(leaderboardService.createLeaderboard(event)).thenReturn(leaderboardInfo.getId());

        boolean result = sagaService.stepCreateLeaderboard(event, sagaState.getId());

        assertTrue(result);
        verify(sagaRepository).save(any(SagaControllingState.class));
        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void testStepCreateLeaderboard_MutableWithoutEvents_Throws() {
        event.setMutable(true);
        event.setEvents(Collections.emptyMap());

        assertThrows(IllegalArgumentException.class,
                () -> sagaService.stepCreateLeaderboard(event, sagaState.getId()));
    }

    @Test
    void testStepSagaCompleted() {
        UserAddedLeaderboard userAdded = new UserAddedLeaderboard();
        userAdded.setLbId(leaderboardInfo.getId());
        userAdded.setUserId(request.getOwnerId());

        sagaService.stepSagaCompleted(userAdded, sagaState.getId());

        verify(stringRedisTemplate).execute(eq(sagaSuccessfulScript), anyList(), eq(leaderboardInfo.getId()),
                eq(SagaStep.COMPLETE.name()), eq(SagaStep.USER_PROFILE_UPDATE.name()));
        verify(leaderboardService).confirmLbCreation(leaderboardInfo.getId());
    }

    @Test
    void testCompensateStepUserProfile_ForImmutable() {
        sagaState.setStatus(SagaStep.USER_PROFILE_UPDATE);
        sagaState.setLeaderboardId(leaderboardInfo.getId());

        leaderboardInfo.setMutable(false);
        when(sagaRepository.findById(sagaState.getId())).thenReturn(Optional.of(sagaState));
        when(leaderboardInfoRepository.findById(leaderboardInfo.getId())).thenReturn(Optional.of(leaderboardInfo));

        sagaService.compensateStepUserProfile(sagaState.getId(), new FailedLeaderboardCreation());

        verify(sagaRepository).save(any(SagaControllingState.class));
        verify(kafkaTemplate, never()).send(any(ProducerRecord.class));
    }

    @Test
    void testCompensateStepUserProfile_ForMutable() {
        sagaState.setStatus(SagaStep.USER_PROFILE_UPDATE);
        sagaState.setLeaderboardId(leaderboardInfo.getId());

        leaderboardInfo.setMutable(true);
        when(sagaRepository.findById(sagaState.getId())).thenReturn(Optional.of(sagaState));
        when(leaderboardInfoRepository.findById(leaderboardInfo.getId())).thenReturn(Optional.of(leaderboardInfo));

        sagaService.compensateStepUserProfile(sagaState.getId(), new FailedLeaderboardCreation());

        verify(sagaRepository).save(any(SagaControllingState.class));
        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void testCompensateStepOptionalEvent_FailedLeaderboardCreation() {
        sagaState.setLeaderboardId(leaderboardInfo.getId());
        sagaState.setStatus(SagaStep.OPTIONAL_EVENTS_CREATE);

        when(sagaRepository.findById(sagaState.getId())).thenReturn(Optional.of(sagaState));

        sagaService.compensateStepOptionalEvent(sagaState.getId(), leaderboardInfo.getId());

        verify(sagaRepository).save(any(SagaControllingState.class));
    }

    @Test
    void testStepCompensateLeaderboard_DeletesLeaderboard() {
        sagaState.setStatus(SagaStep.COMPENSATE_LEADERBOARD);
        sagaState.setLeaderboardId(leaderboardInfo.getId());
        when(sagaRepository.findById(sagaState.getId())).thenReturn(Optional.of(sagaState));

        sagaService.stepCompensateLeaderboard(new DeleteLbEvent(leaderboardInfo.getId()), sagaState.getId());

        verify(leaderboardService).deleteLeaderboard(eq(leaderboardInfo.getId()), eq(sagaState.getId()));
    }

    @Test
    void testStepOutOfTime_LeaderboardCreate() {
        sagaState.setStatus(SagaStep.LEADERBOARD_CREATE);
        sagaState.setLeaderboardId(leaderboardInfo.getId());
        when(sagaRepository.findByLeaderboardId(leaderboardInfo.getId())).thenReturn(List.of(sagaState));

        boolean result = sagaService.stepOutOfTime(leaderboardInfo.getId());

        verify(leaderboardService).deleteLeaderboard(leaderboardInfo.getId(), sagaState.getId());
        assertTrue(result);
    }
}