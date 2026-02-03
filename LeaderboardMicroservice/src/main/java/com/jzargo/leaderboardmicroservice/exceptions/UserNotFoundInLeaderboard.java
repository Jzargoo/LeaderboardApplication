package com.jzargo.leaderboardmicroservice.exceptions;

public class UserNotFoundInLeaderboard extends RuntimeException {
    public UserNotFoundInLeaderboard(String message) {
        super(message);
    }
}
