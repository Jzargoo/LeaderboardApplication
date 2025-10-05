package com.jzargo.messaging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class UserScoreUploadEvent {
    private String lbId;
    private String username;
    private Long userId;
    private String region;
    private double score;
    private Map<String, Object> metadata;
}