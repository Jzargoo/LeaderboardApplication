package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor @AllArgsConstructor
@Builder @Data
public class LeaderboardEventInitialization {
    private Map<String, Object> metadata;
    private Map<String, Double> events;
    private boolean isPublic;
    private long userId;
    private String lbId;
}
