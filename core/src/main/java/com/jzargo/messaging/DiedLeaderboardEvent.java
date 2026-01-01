package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor @NoArgsConstructor
public class DiedLeaderboardEvent {
    private String leaderboardName;
    private String leaderboardId;
    private Long ownerId;
    private String description;
    private Map<Long, Double> leaderboardFinalState;
}
