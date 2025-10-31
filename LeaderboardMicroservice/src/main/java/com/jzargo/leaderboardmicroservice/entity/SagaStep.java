package com.jzargo.leaderboardmicroservice.entity;

public enum SagaStep {
    INITIATED,
    LEADERBOARD_CREATED,
    OPTIONAL_EVENTS_CREATED,
    USER_PROFILE_UPDATED,
    STATISTICS_UPDATED,
    COMPLETED,
    FAILED
}
