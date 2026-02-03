package com.jzargo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserScoreResponse {
    private Long userId;
    private String lbId;
    private Double score;
    private Long rank;
}
