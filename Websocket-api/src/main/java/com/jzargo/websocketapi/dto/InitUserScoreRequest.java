package com.jzargo.websocketapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InitUserScoreRequest {
    private String leaderboardId;
}
