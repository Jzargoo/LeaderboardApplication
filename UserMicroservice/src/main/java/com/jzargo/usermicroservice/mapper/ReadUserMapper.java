package com.jzargo.usermicroservice.mapper;

import com.jzargo.mapper.Mapper;
import com.jzargo.usermicroservice.api.UserResponse;
import com.jzargo.usermicroservice.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ReadUserMapper implements Mapper<User, UserResponse> {
    @Override
    public UserResponse map(User from) {
        return UserResponse.builder()
                .userId(from.getId())
                .name(from.getName())
                .regionCode(from.getRegion())
                .activeLeaderboards(from.getActiveLeaderboards())
                .build();
    }
}
