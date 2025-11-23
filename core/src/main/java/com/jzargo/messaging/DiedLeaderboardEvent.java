package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor @NoArgsConstructor
public class DiedLeaderboardEvent {
    String leaderboardName;
    Long ownerId;
    String description;
    Map<Long, Double> leaderboardFinalState;
}
