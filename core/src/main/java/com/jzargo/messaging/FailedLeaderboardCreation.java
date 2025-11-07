package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor
@Data
public class FailedLeaderboardCreation {
    private String lbId;
    private String reason;
    private Long userId;
}
