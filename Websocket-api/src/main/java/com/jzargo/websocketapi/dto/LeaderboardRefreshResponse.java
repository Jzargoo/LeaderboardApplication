package com.jzargo.websocketapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardRefreshResponse {
    private Long userId;
    private String lbId;
    private Double score;
    private Long rank;
    private Map<Long, Double> globalRange;
    String description;
    String name;
    String leaderboardId;

}
