package com.jzargo.gatewayapi.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserScoreResponse {
    private Long userId;
    private Double score;
}
