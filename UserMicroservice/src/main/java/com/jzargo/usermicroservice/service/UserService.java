package com.jzargo.usermicroservice.service;

import com.jzargo.messaging.ActiveLeaderboardEvent;
import com.jzargo.messaging.DiedLeaderboardEvent;
import com.jzargo.usermicroservice.api.UserRegisterRequest;
import com.jzargo.usermicroservice.api.model.UserResponse;

public interface UserService {
    void changeAvatar(byte[] avatar, long userId);

    void register(UserRegisterRequest request);

    boolean deleteUser(Long id);

    void updateUser(Long id, UserRegisterRequest request);

    UserResponse findById(Long id);

    void addActiveLeaderboard(ActiveLeaderboardEvent event);

    void removeLeaderboard(DiedLeaderboardEvent event);
}
