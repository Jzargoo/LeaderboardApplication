package com.jzargo.leaderboardmicroservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitUserScoreRequest {
    private String leaderboardId;
}
