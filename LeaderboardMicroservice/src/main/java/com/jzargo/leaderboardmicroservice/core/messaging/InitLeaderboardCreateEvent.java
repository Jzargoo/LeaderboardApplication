package com.jzargo.leaderboardmicroservice.core.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitLeaderboardCreateEvent {
    private long ownerId;
    private String username;
    private Set<String> regions;
    private String description;
    private int globalRange;
    private String lbId;
    private String nameLb;
    private int initialValue;
    private boolean isPublic;
    private boolean isMutable;
    private LocalDateTime expireAt;
    private double maxScore;
    private int maxEventsPerUser;
    private int maxEventsPerUserPerDay;
    private boolean showTies;
    private Map<String, Double> events;
}