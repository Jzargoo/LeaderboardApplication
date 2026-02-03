package com.jzargo.websocketapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardResponsePayload {
    private String leaderboardId;
    private Long userId;
    private Double score;
    private Long rank;
}
