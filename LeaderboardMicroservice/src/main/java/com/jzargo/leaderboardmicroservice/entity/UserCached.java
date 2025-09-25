package com.jzargo.leaderboardmicroservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.HashMap;
import java.util.Map;

@RedisHash("user_cached")
@Builder
@AllArgsConstructor @NoArgsConstructor
@Data
public class UserCached {
    @Id
    private Long id;
    private String username;
    @Builder.Default
    private Map<String, Integer> totalAttempts = new HashMap<>();
    private String region;

}
