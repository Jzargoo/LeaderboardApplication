package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserScoreEvent {
    private double score;
    private Long userId;
    private String lbId;
    private Map<String, Object> metadata;
}
