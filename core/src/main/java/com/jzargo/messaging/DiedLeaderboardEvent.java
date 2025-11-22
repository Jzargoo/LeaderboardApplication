package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor @NoArgsConstructor
public class DiedLeaderboardEvent {
    String leaderboardName;
    String description;
    Map<Long, Double> leaderboardFinalState; //TODO: now that it have map, the way of consuming in user microservice is deprecated.
}
