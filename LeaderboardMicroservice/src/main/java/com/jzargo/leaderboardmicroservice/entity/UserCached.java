package com.jzargo.leaderboardmicroservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.Map;

@RedisHash("user_cached")
@Data
public class UserCached {
    @Id
    private Long id;
    private String username;
    private Map<String, Integer> attempts;
}
