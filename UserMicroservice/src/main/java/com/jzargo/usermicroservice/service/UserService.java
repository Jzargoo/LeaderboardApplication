package com.jzargo.usermicroservice.service;

import com.jzargo.messaging.ActiveLeaderboardEvent;
import com.jzargo.usermicroservice.api.UserRegisterRequest;
import com.jzargo.usermicroservice.api.UserResponse;

public interface UserService {
    void changeAvatar(byte[] avatar, long userId);

    void register(UserRegisterRequest request);

    UserResponse findById(Long id);

    void addActiveLeaderboard(ActiveLeaderboardEvent event);
}
