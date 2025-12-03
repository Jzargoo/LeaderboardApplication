package com.jzargo.websocketapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinResponse {
    Map<Long, Double> leaderboard;
    String name;
    String description;
}
