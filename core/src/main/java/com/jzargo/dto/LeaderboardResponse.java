package com.jzargo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaderboardResponse {
    Map<Long, Double> leaderboard;
    String description;
    String name;
    String leaderboardId;
}
