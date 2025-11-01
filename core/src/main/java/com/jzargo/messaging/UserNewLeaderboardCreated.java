package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor @NoArgsConstructor
public class UserNewLeaderboardCreated {
    private String lbId;
    private String name;
    private Long userId;
    private String description;
}
