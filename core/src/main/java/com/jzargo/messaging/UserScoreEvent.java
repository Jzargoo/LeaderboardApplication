package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserScoreEvent {
    private double score;

    private String username;
    private Long userId;
    private String region;
    private String lbId;
    private Map<String, Object> metadata;
}
