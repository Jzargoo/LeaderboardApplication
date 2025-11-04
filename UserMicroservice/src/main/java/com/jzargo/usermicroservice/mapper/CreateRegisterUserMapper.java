package com.jzargo.usermicroservice.mapper;

import com.jzargo.mapper.Mapper;
import com.jzargo.messaging.UserRegisterRequest;
import com.jzargo.usermicroservice.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CreateRegisterUserMapper implements Mapper<UserRegisterRequest, User> {
    @Value("${images.dump.user}")
    private String avatarDump;
    @Override
    public User map(UserRegisterRequest request){
        return User.builder()
                .id(request.getUserId())
                .email(request.getEmail())
                .name(request.getName())
                .avatar(avatarDump)
                .build();
    }
}
