package com.jzargo.leaderboardmicroservice.unit;

import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.exceptions.CannotCreateCachedUserException;
import com.jzargo.leaderboardmicroservice.mapper.MapperCreateLeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardServiceImpl;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import com.jzargo.region.Regions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaderboardServiceUnitTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private LeaderboardInfoRepository leaderboardInfoRepository;

    @Mock
    private RedisScript<String> mutableLeaderboardScript;

    @Mock
    private RedisScript<String> immutableLeaderboardScript;

    @Mock
    private RedisScript<String> createLeaderboardScript;

    @Mock
    private MapperCreateLeaderboardInfo mapperCreateLeaderboardInfo;


    @Mock
    private RedisScript<String> createUserCachedScript;

    @Mock
    private RedisScript<String> deleteLeaderboardScript;

    @Mock
    private RedisScript<String> confirmLbCreationScript;

    @Mock
    private SagaControllingStateRepository sagaControllingStateRepository;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private LeaderboardServiceImpl leaderboardService;

    private LeaderboardInfo testLeaderboardInfo;
    private UserScoreEvent userScoreEvent;

    private final long USER_ID = 1L;

    @BeforeEach
    public void setUp() {

        testLeaderboardInfo = LeaderboardInfo.builder()
                .id("lb-123")
                .name("Top Players")
                .description("Leaderboard for top scoring users")
                .ownerId(42L)
                .globalRange(20)
                .initialValue(100.0)
                .isPublic(true)
                .isMutable(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .expireAt(LocalDateTime.now().plusDays(30))
                .maxScore(1000.0)
                .regions(Regions.GLOBAL.getCode())
                .maxEventsPerUser(5)
                .isActive(true)
                .maxEventsPerUserPerDay(2)
                .showTies(true)
                .build();


        userScoreEvent = new UserScoreEvent(
                10.0,
                1L,
                testLeaderboardInfo.getId(),
                Map.of()
        );

    }

    @Test
    public void test_increaseUserScore_Successful() throws CannotCreateCachedUserException {

        when(leaderboardInfoRepository
                .findById(anyString())
        ).thenReturn(Optional.of(testLeaderboardInfo));

        when(stringRedisTemplate
                .execute(
                        any(),
                        anyList(),
                        any(Object[].class)
                )
        ).thenAnswer(invocation -> "success");

        setWhetherUserCached(true);

        leaderboardService.increaseUserScore(userScoreEvent);


        verify(stringRedisTemplate, times(1))
                .execute(
                        any(),
                        anyList(),
                        any(Object[].class)
                );
    }
    @Test
    public void increaseUserScore_UserNotCached_CreationFails() {

        when(stringRedisTemplate.execute(
                any(),
                anyList(),
                any(Object[].class)
        )).thenReturn("error");

        assertThrows(CannotCreateCachedUserException.class,
                () -> leaderboardService.increaseUserScore(userScoreEvent));
    }

    @Test
    public void test_addNewScore_Successful() {
        UserScoreUploadEvent uploadEvent = new UserScoreUploadEvent(
                testLeaderboardInfo.getId(), USER_ID,
                50.0, Map.of()
        );

        when(
                leaderboardInfoRepository.findById(anyString())
        ).thenReturn(Optional.of(testLeaderboardInfo));

        when(
                stringRedisTemplate
                        .execute(
                                any(),
                                anyList(),
                                any(Object[].class))
        ).thenReturn("success");

        setWhetherUserCached(true);

        leaderboardService.addNewScore(uploadEvent);

        verify(stringRedisTemplate, times(1)).execute(any(), anyList(), any(Object[].class));
    }

    @Test
    public void test_createLeaderboard_Successful() {
        InitLeaderboardCreateEvent request = new InitLeaderboardCreateEvent();
        request.setMaxScore(500.0);
        request.setInitialValue(100.0);

        LeaderboardInfo mappedInfo = testLeaderboardInfo;
        mappedInfo.setId("lb-999");

        when(mapperCreateLeaderboardInfo.map(any())).thenReturn(mappedInfo);
        when(stringRedisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn("OK");

        String lbId = leaderboardService.createLeaderboard(request);
        assertEquals("lb-999", lbId);
    }

    @Test
    public void test_initUserScore_Successful() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        InitUserScoreRequest request = new InitUserScoreRequest();
        request.setLeaderboardId(testLeaderboardInfo.getId());

        setWhetherUserCached(true);

        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        when(leaderboardInfoRepository
                .findById(anyString() )
        ).thenReturn(Optional.of(testLeaderboardInfo));

        leaderboardService.initUserScore(request, USER_ID);

        verify(stringRedisTemplate.opsForZSet(), times(1))
                .add(eq(testLeaderboardInfo.getKey()), eq(String.valueOf(USER_ID)), eq(testLeaderboardInfo.getInitialValue()));
    }

    @Test
    public void test_deleteLeaderboard_Successful() {
        String sagaId = "saga-123";
        SagaControllingState sagaState = new SagaControllingState();
        sagaState.setId(sagaId);

        when(leaderboardInfoRepository.findById(anyString())).thenReturn(Optional.of(testLeaderboardInfo));
        when(sagaControllingStateRepository.findById(anyString())).thenReturn(Optional.of(sagaState));
        when(stringRedisTemplate.execute(any(), anyList())).thenReturn("OK");

        leaderboardService.deleteLeaderboard(testLeaderboardInfo.getId(), sagaId);

        verify(stringRedisTemplate, times(1)).execute(any(), anyList(), any(Object[].class));
    }

    @Test
    public void test_confirmLbCreation_Successful() {
        when(leaderboardInfoRepository.findById(anyString())).thenReturn(Optional.of(testLeaderboardInfo));
        when(stringRedisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn("OK");

        leaderboardService.confirmLbCreation(testLeaderboardInfo.getId());

        verify(stringRedisTemplate, times(1))
                .execute(any(), anyList(), any(Object[].class));
    }

    private void setWhetherUserCached(boolean isCached) {
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(hashOperations
                .hasKey(
                        anyString(),
                        anyString())
        ).thenReturn(isCached);
        when(stringRedisTemplate.opsForHash())
                .thenReturn(hashOperations);
    }
}