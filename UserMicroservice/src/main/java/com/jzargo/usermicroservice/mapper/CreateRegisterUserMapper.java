package com.jzargo.usermicroservice.mapper;

import com.jzargo.mapper.Mapper;
import com.jzargo.messaging.UserRegisterRequest;
import com.jzargo.usermicroservice.config.properties.ApplicationPropertiesStorage;
import com.jzargo.usermicroservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateRegisterUserMapper implements Mapper<UserRegisterRequest, User> {
    private final ApplicationPropertiesStorage applicationPropertiesStorage;
    @Override
    public User map(UserRegisterRequest request){
        return User.builder()
                .id(request.getUserId())
                .email(request.getEmail())
                .name(request.getName())
                .avatar(
                        applicationPropertiesStorage.getImages()
                                .getDump().getUser()
                )
                .build();
    }
}
