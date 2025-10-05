package com.jzargo.messaging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor @Data
public class UserScoreEvent {
    private double score;
    private String username;
    private Long userId;
    private String region;
    private String lbId;
    private Map<String, Object> metadata;
}
