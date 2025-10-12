package com.jzargo.leaderboardmicroservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor @NoArgsConstructor
@RedisHash("leaderboard_information")
public class LeaderboardInfo {
    @Id
    private String id;
    private String description;
    private long ownerId;
    @Builder.Default
    private int globalRange = 10;
    private String name;
    private double initialValue;
    private boolean isPublic;
    private boolean isMutable;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime expireAt = LocalDateTime.now().plusDays(15);
    private double maxScore;
    @Builder.Default
    private Set<String> regions = new HashSet<>();
    private int maxEventsPerUser;
    private int maxEventsPerUserPerDay;
    private boolean showTies;

}
