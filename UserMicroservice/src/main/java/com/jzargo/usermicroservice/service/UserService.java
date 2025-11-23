package com.jzargo.usermicroservice.service;

import com.jzargo.messaging.*;
import com.jzargo.usermicroservice.api.model.UserResponse;
import com.jzargo.usermicroservice.exception.UserCannotCreateLeaderboardException;

public interface UserService {
    void changeAvatar(byte[] avatar, long userId);

    void register(UserRegisterRequest request);

    boolean deleteUser(Long id);

    void updateUser(Long id, UserRegisterRequest request);

    UserResponse findById(Long id);

    // Method which add to profile leaderboard that has been started by a user
    void addActiveLeaderboard(ActiveLeaderboardEvent event);
    // Method which remove from profile leaderboard that ended in active section, not created
    void removeLeaderboard(DiedLeaderboardEvent event);
    // Method which add to profile leaderboard  that has been created by a user
    void addCreatedLeaderboard(UserNewLeaderboardCreated userNewLeaderboardCreated) throws UserCannotCreateLeaderboardException;

    void removeCreatedLeaderboard(OutOfTimeEvent outOfTimeEvent );
}
