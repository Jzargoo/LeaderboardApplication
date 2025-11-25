package com.jzargo.leaderboardmicroservice.mapper;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.mapper.Mapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class LeaderboardToResponseReadMapper implements Mapper<LeaderboardInfo, LeaderboardResponse> {
    private final StringRedisTemplate stringRedisTemplate;

    public LeaderboardToResponseReadMapper(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public LeaderboardResponse map(LeaderboardInfo from) {
        Map<Long, Double> leaderboard = Objects.requireNonNull(stringRedisTemplate
                        .opsForZSet()
                        .rangeWithScores(from.getKey(), 0, -1))
                .stream()
                .collect(Collectors.toMap(
                        tuple -> Long.valueOf(tuple.getValue()),
                        ZSetOperations.TypedTuple::getScore,
                        (a,b) -> a,
                        LinkedHashMap::new
                ));
        return new LeaderboardResponse(leaderboard, from.getDescription(), from.getName());
    }
}
