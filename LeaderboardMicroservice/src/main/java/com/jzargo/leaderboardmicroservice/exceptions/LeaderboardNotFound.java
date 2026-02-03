package com.jzargo.leaderboardmicroservice.exceptions;

public class LeaderboardNotFound extends RuntimeException {
    public LeaderboardNotFound(String message) {
        super(message);
    }
}
