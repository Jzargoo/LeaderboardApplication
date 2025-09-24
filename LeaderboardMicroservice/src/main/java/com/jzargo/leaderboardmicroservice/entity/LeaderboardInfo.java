package com.jzargo.leaderboardmicroservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;

@Data
@RedisHash("leaderboard_information")
public class LeaderboardInfo {
    @Id
    private String id;
    private String description;
    private long ownerId;
    private String name;
    private boolean isPublic;
    private int initialValue;
    private LocalDateTime createdAt;
    private LocalDateTime expireAt;
    private int maxEntries;
    private int maxScore;
    private int maxEventsPerUser;
    private int maxEventsPerUserPerDay;
    private String region;
    private boolean showTies;
}
