package com.jzargo.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor @NoArgsConstructor
public class DiedLeaderboardEvent {
    String leaderboardName;
    List<Long> userIds;
}
