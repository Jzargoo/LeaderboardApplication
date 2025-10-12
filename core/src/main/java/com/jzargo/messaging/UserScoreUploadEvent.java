package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserScoreUploadEvent {
    private String lbId;
    private String username;
    private Long userId;
    private String region;
    private double score;
    private Map<String, Object> metadata;
}