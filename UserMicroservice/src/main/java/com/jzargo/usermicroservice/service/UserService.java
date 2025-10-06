package com.jzargo.usermicroservice.service;

public interface UserService {
    byte[] changeAvatar(byte[] avatar, long userId);
}
