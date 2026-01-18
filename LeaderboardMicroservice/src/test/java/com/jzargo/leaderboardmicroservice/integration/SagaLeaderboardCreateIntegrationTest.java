package com.jzargo.leaderboardmicroservice.integration;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.entity.SagaStep;
import com.jzargo.leaderboardmicroservice.handler.KafkaSagaLeaderboardCreationHandler;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.SuccessfulEventInitialization;
import com.jzargo.messaging.UserAddedLeaderboard;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import com.jzargo.region.Regions;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@EmbeddedKafka
@DirtiesContext
@ActiveProfiles("test")
@Import(TestConfigHelper.class)
@Testcontainers
@SpringBootTest(properties = "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}")
public class SagaLeaderboardCreateIntegrationTest {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:latest")
    ).withExposedPorts(6379);


    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaPropertyStorage kafkaPropertyStorage;

    @Autowired
    private SagaControllingStateRepository sagaControllingStateRepository;

    @Autowired
    private SagaTestConsumer sagaTestConsumer;

    @Autowired
    private SagaLeaderboardCreate saga;


    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getFirstMappedPort().toString());
    }


    @MockitoSpyBean
    KafkaSagaLeaderboardCreationHandler kafkaSagaLeaderboardCreationHandler;



    @Test
    public void fullSagaSuccessFlow() throws InterruptedException {
        // ===== GIVEN =====
        CreateLeaderboardRequest req = CreateLeaderboardRequest.builder()
                .name("Top Players")
                .description("Leaderboard for top players")
                .globalRange(10)
                .ownerId(100L)
                .initialValue(0)
                .isPublic(true)
                .isMutable(true)
                .expireAt(LocalDateTime.now().plusDays(7))
                .maxScore(1000.0)
                .regions(Set.of(Regions.GLOBAL.getCode()))
                .maxEventsPerUser(10)
                .maxEventsPerUserPerDay(3)
                .showTies(true)
                .events(Map.of("EventA", 10.0, "EventB", 5.0))
                .build();

        // ===== WHEN =====
        saga.startSaga(req, 100L, "jack");

        // ===== CAPTORS =====
        ArgumentCaptor<InitLeaderboardCreateEvent> initCaptor =
                ArgumentCaptor.forClass(InitLeaderboardCreateEvent.class);
        ArgumentCaptor<SuccessfulEventInitialization> successCaptor =
                ArgumentCaptor.forClass(SuccessfulEventInitialization.class);
        ArgumentCaptor<UserAddedLeaderboard> userAddedCaptor =
                ArgumentCaptor.forClass(UserAddedLeaderboard.class);
        ArgumentCaptor<String> sagaIdCaptor =
                ArgumentCaptor.forClass(String.class);

        // ===== VERIFY CreateLeaderboardSaga =====
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                verify(kafkaSagaLeaderboardCreationHandler, atLeastOnce())
                        .handleCreateLeaderboardSaga(
                                initCaptor.capture(),
                                sagaIdCaptor.capture(),
                                anyString()
                        )
        );

        String sagaId = sagaIdCaptor.getValue();
        InitLeaderboardCreateEvent initEvent = initCaptor.getValue();

        assertInitEvent(req, initEvent);

        SagaControllingState stateAfterInit = sagaControllingStateRepository
                .findById(sagaId)
                .orElseThrow();

        initEvent.setLbId(stateAfterInit.getLeaderboardId());

        assertEquals(SagaStep.OPTIONAL_EVENTS_CREATE, stateAfterInit.getStatus());
        assertEquals(SagaStep.LEADERBOARD_CREATE.name(), stateAfterInit.getLastStepCompleted());

        LeaderboardEventInitialization pollLbInit =
                sagaTestConsumer.getLeaderboardEventInitializationBlockingQueue().poll(10, TimeUnit.SECONDS);

        assertLeaderboardEventInitialization(initEvent, pollLbInit);

        SuccessfulEventInitialization successfulEvent = new SuccessfulEventInitialization();

        ProducerRecord<String, Object> record =
                KafkaUtils.createRecord(
                        kafkaPropertyStorage.getTopic().getNames().getSagaCreateLeaderboard(),
                        sagaId,
                        successfulEvent);

        KafkaUtils.addSagaHeaders(record, sagaId, KafkaUtils.newMessageId(), sagaId);

        kafkaTemplate.send(record);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                verify(kafkaSagaLeaderboardCreationHandler, atLeastOnce())
                        .handleSuccessfulEventInitialization(
                                sagaIdCaptor.capture(),
                                anyString(),
                                successCaptor.capture()
                        )
        );

        SuccessfulEventInitialization value = successCaptor.getValue();

        String sagaIdInEventInit = sagaIdCaptor.getValue();

        assertSuccessfulEvent(initEvent, sagaId, value, sagaIdInEventInit);

        UserNewLeaderboardCreated pollUser =
                sagaTestConsumer.getUserNewLeaderboardCreatedBlockingQueue().poll(10, TimeUnit.SECONDS);

        assertUserCreated(req, initEvent, pollUser);

        UserAddedLeaderboard userAddedLeaderboardBody =
                new UserAddedLeaderboard(initEvent.getLbId(), initEvent.getOwnerId());

        ProducerRecord<String, Object> successfulUserUpdate =
                KafkaUtils.createRecord(
                        kafkaPropertyStorage.getTopic().getNames().getSagaCreateLeaderboard(),
                        sagaId,
                        userAddedLeaderboardBody);

        KafkaUtils.addSagaHeaders(successfulUserUpdate, sagaId, KafkaUtils.newMessageId(), sagaId);

        kafkaTemplate.send(successfulUserUpdate);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                verify(kafkaSagaLeaderboardCreationHandler, atLeastOnce())
                        .handleUserAddedLeaderboard(
                                userAddedCaptor.capture(),
                                sagaIdCaptor.capture(),
                                anyString()
                        )
        );

        UserAddedLeaderboard userAddedLeaderboard = userAddedCaptor.getValue();

        String sagaIdAfterUser = sagaIdCaptor.getValue();

        assertUserAdded(req, initEvent, userAddedLeaderboard, sagaIdAfterUser, sagaId);

        SagaControllingState stateAfterAddedLb = sagaControllingStateRepository.findById(sagaId).orElseThrow();

        assertEquals(SagaStep.COMPLETE, stateAfterAddedLb.getStatus());

        assertEquals(SagaStep.USER_PROFILE_UPDATE.name(), stateAfterAddedLb.getLastStepCompleted());
    }

    // ===== VERIFY METHODS =====

    public void assertInitEvent(CreateLeaderboardRequest req, InitLeaderboardCreateEvent initEvent) {
        assertNotNull(initEvent);
        assertEquals(req.getOwnerId(), initEvent.getOwnerId());
        assertEquals("jack", initEvent.getUsername());
        assertEquals(req.getDescription(), initEvent.getDescription());
        assertEquals(req.getGlobalRange(), initEvent.getGlobalRange());
        assertEquals(req.getInitialValue(), initEvent.getInitialValue());
        assertEquals(req.isPublic(), initEvent.isPublic());
        assertEquals(req.isMutable(), initEvent.isMutable());
        assertEquals(req.isShowTies(), initEvent.isShowTies());
        assertEquals(req.getMaxScore(), initEvent.getMaxScore());
        assertEquals(req.getMaxEventsPerUser(), initEvent.getMaxEventsPerUser());
        assertEquals(req.getMaxEventsPerUserPerDay(), initEvent.getMaxEventsPerUserPerDay());
        assertEquals(req.getRegions(), initEvent.getRegions());
        assertEquals(req.getEvents(), initEvent.getEvents());
        assertEquals(req.getExpireAt(), initEvent.getExpireAt());
    }

    public void assertLeaderboardEventInitialization(InitLeaderboardCreateEvent initEvent,
                                                     LeaderboardEventInitialization lbInit) {
        assertNotNull(lbInit);
        assertEquals(initEvent.getLbId(), lbInit.getLbId());
        assertEquals(initEvent.getOwnerId(), lbInit.getUserId());
        assertEquals(initEvent.isPublic(), lbInit.isPublic());
        assertEquals(initEvent.getEvents(), lbInit.getEvents());
        assertNotNull(lbInit.getMetadata());
        assertFalse(lbInit.getMetadata().isEmpty());
    }

    public void assertSuccessfulEvent(InitLeaderboardCreateEvent initEvent, String sagaId,
                                      SuccessfulEventInitialization successEvent, String sagaIdCaptured) {
        assertNotNull(successEvent);
        assertEquals(sagaId, sagaIdCaptured);
        assertEquals(initEvent.getOwnerId(), successEvent.getUserId());
        assertEquals(initEvent.getLbId(), successEvent.getLbId());
    }

    public void assertUserCreated(CreateLeaderboardRequest req, InitLeaderboardCreateEvent initEvent,
                                  UserNewLeaderboardCreated pollUser) {
        assertNotNull(pollUser);
        assertEquals(req.getOwnerId(), pollUser.getUserId());
        assertEquals(req.getName(), pollUser.getName());
        assertEquals(initEvent.getLbId(), pollUser.getLbId());
    }

    public void assertUserAdded(CreateLeaderboardRequest req, InitLeaderboardCreateEvent initEvent,
                                UserAddedLeaderboard userAdded, String sagaIdCaptured, String sagaId) {
        assertNotNull(userAdded);
        assertEquals(sagaId, sagaIdCaptured);
        assertEquals(req.getOwnerId(), userAdded.getUserId());
        assertEquals(initEvent.getLbId(), userAdded.getLbId());
    }

}