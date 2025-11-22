package com.jzargo.leaderboardmicroservice.unit;

import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.entity.UserCached;
import com.jzargo.leaderboardmicroservice.exceptions.CannotCreateCachedUserException;
import com.jzargo.leaderboardmicroservice.mapper.MapperCreateLeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.CachedUserRepository;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardServiceImpl;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import com.jzargo.messaging.UserUpdateEvent;
import com.jzargo.region.Regions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private CachedUserRepository cachedUserRepository;

    @Mock
    private RedisScript<String> createUserCachedScript;

    @Mock
    private RedisScript<String> deleteLeaderboardScript;

    @Mock
    private RedisScript<String> confirmLbCreationScript;

    @Mock
    private SagaControllingStateRepository sagaControllingStateRepository;

    @InjectMocks
    private LeaderboardServiceImpl leaderboardService;

    private LeaderboardInfo testLeaderboardInfo;
    private UserCached userCached;
    private UserScoreEvent userScoreEvent;

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

        userCached = new UserCached(11L, "user", Map.of(), Regions.GLOBAL.getCode());

        userScoreEvent = new UserScoreEvent(
                10.0,
                userCached.getUsername(),
                userCached.getId(),
                userCached.getRegion(),
                testLeaderboardInfo.getId(),
                Map.of()
        );
    }

    @Test
    public void test_increaseUserScore_Successful() throws CannotCreateCachedUserException {
        when(cachedUserRepository
                .existsById(anyLong())
        ).thenReturn(true);

        when(leaderboardInfoRepository
                .findById(anyString())
        ).thenReturn(Optional.of(testLeaderboardInfo));

        when(cachedUserRepository
                .findById(anyLong())
        ).thenReturn(Optional.of(userCached));

        when(stringRedisTemplate
                        .execute(
                                eq(mutableLeaderboardScript),
                                anyList(),
                                any(Object[].class))
        ).thenReturn("success");

        leaderboardService.increaseUserScore(userScoreEvent);

        verify(stringRedisTemplate, times(1))
                .execute(eq(mutableLeaderboardScript), anyList(), any(Object[].class));
    }

    @Test
    public void increaseUserScore_UserNotCached_CreationFails() {
        when(cachedUserRepository.existsById(anyLong())).thenReturn(false);

        lenient().when(stringRedisTemplate.execute(
                eq(createUserCachedScript),
                anyList(),
                any(Object[].class)
        )).thenReturn("error");

        assertThrows(IllegalStateException.class,
                () -> leaderboardService.increaseUserScore(userScoreEvent));
    }

    @Test
    public void test_addNewScore_Successful() {
        UserScoreUploadEvent uploadEvent = new UserScoreUploadEvent(
                testLeaderboardInfo.getId(), userCached.getUsername(), userCached.getId(), userCached.getRegion(),
                50.0, Map.of()
        );

        when(cachedUserRepository.existsById(anyLong())).thenReturn(true);
        when(leaderboardInfoRepository.findById(anyString())).thenReturn(Optional.of(testLeaderboardInfo));
        when(cachedUserRepository.findById(anyLong())).thenReturn(Optional.of(userCached));
        when(stringRedisTemplate.execute(eq(mutableLeaderboardScript), anyList(), any(Object[].class))).thenReturn("success");

        leaderboardService.addNewScore(uploadEvent);

        verify(stringRedisTemplate, times(1)).execute(eq(mutableLeaderboardScript), anyList(), any(Object[].class));
    }

    @Test
    public void test_createLeaderboard_Successful() {
        InitLeaderboardCreateEvent request = new InitLeaderboardCreateEvent();
        request.setOwnerId(userCached.getId());
        request.setUsername(userCached.getUsername());
        request.setMaxScore(500.0);
        request.setInitialValue(100.0);

        LeaderboardInfo mappedInfo = testLeaderboardInfo;
        mappedInfo.setId("lb-999");

        when(cachedUserRepository.existsById(anyLong())).thenReturn(true);
        when(mapperCreateLeaderboardInfo.map(any())).thenReturn(mappedInfo);
        when(stringRedisTemplate.execute(eq(createLeaderboardScript), anyList(), any(Object[].class))).thenReturn("OK");

        String lbId = leaderboardService.createLeaderboard(request, userCached.getRegion());
        assertEquals("lb-999", lbId);
    }

    @Test
    public void test_initUserScore_Successful() {
        InitUserScoreRequest request = new InitUserScoreRequest();
        request.setLeaderboardId(testLeaderboardInfo.getId());

        when(leaderboardInfoRepository.findById(anyString())).thenReturn(Optional.of(testLeaderboardInfo));
        when(cachedUserRepository.existsById(anyLong())).thenReturn(true);

        leaderboardService.initUserScore(request, userCached.getUsername(), userCached.getId(), userCached.getRegion());

        verify(stringRedisTemplate.opsForZSet(), times(1))
                .add(eq(testLeaderboardInfo.getKey()), eq(String.valueOf(userCached.getId())), eq(testLeaderboardInfo.getInitialValue()));
    }

    @Test
    public void test_updateUserCache_Successful() {
        UserUpdateEvent updateEvent = new UserUpdateEvent();
        updateEvent.setId(userCached.getId());
        updateEvent.setName("newName");
        updateEvent.setRegion(Regions.GLOBAL);

        when(cachedUserRepository.findById(anyLong())).thenReturn(Optional.of(userCached));
        when(cachedUserRepository.save(any())).thenReturn(userCached);

        leaderboardService.updateUserCache(updateEvent);

        assertEquals("newName", userCached.getUsername());
        assertEquals(Regions.GLOBAL.getCode(), userCached.getRegion());
        verify(cachedUserRepository, times(1)).save(userCached);
    }

    @Test
    public void test_deleteLeaderboard_Successful() {
        String sagaId = "saga-123";
        SagaControllingState sagaState = new SagaControllingState();
        sagaState.setId(sagaId);

        when(leaderboardInfoRepository.findById(anyString())).thenReturn(Optional.of(testLeaderboardInfo));
        when(sagaControllingStateRepository.findById(anyString())).thenReturn(Optional.of(sagaState));
        when(stringRedisTemplate.execute(eq(deleteLeaderboardScript), anyList())).thenReturn("OK");

        leaderboardService.deleteLeaderboard(testLeaderboardInfo.getId(), sagaId);

        verify(stringRedisTemplate, times(1)).execute(eq(deleteLeaderboardScript), anyList());
    }

    @Test
    public void test_confirmLbCreation_Successful() {
        when(leaderboardInfoRepository.findById(anyString())).thenReturn(Optional.of(testLeaderboardInfo));
        when(stringRedisTemplate.execute(eq(confirmLbCreationScript), anyList(), any(Object[].class))).thenReturn("OK");

        leaderboardService.confirmLbCreation(testLeaderboardInfo.getId());

        verify(stringRedisTemplate, times(1))
                .execute(eq(confirmLbCreationScript), anyList(), any(Object[].class));
    }
}