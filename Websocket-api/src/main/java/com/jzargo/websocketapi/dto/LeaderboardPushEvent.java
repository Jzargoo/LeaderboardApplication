package com.jzargo.websocketapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardPushEvent <T>{
    PushEventType type;
    T payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncreaseUserScore{
        Long userId;
        String event;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateLeaderboardScore{
        Long userId;
        Double newScore;
    }
}
