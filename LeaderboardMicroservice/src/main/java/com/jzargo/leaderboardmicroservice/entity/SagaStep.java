package com.jzargo.leaderboardmicroservice.entity;

public enum SagaStep {
    INITIATED,
    LEADERBOARD_CREATED,
    OPTIONAL_EVENTS_CREATED,
    USER_PROFILE_UPDATE,
    COMPLETED,
    COMPENSATE_USER_PROFILE,
    COMPENSATE_OPTIONAL_EVENT,
    FAILED
}
