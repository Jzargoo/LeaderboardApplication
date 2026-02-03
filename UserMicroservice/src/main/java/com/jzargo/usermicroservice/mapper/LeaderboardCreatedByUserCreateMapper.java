package com.jzargo.usermicroservice.mapper;

import com.jzargo.mapper.Mapper;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import com.jzargo.usermicroservice.entity.LeaderboardCreatedByUser;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardCreatedByUserCreateMapper implements Mapper<UserNewLeaderboardCreated,LeaderboardCreatedByUser> {

    @Override
    public LeaderboardCreatedByUser map(UserNewLeaderboardCreated from) {

        return LeaderboardCreatedByUser.builder()
                .lbId(from.getLbId())
                .userId(from.getUserId())
                .build();
    }
}
