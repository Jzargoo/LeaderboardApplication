package com.jzargo.leaderboardmicroservice.mapper;

import com.jzargo.mapper.Mapper;
import com.jzargo.messaging.InitLeaderboardCreateEvent;
import com.jzargo.messaging.LeaderboardEventInitialization;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CreateLeaderboardEventInitializationCreateMapper implements Mapper<InitLeaderboardCreateEvent, LeaderboardEventInitialization> {
    @Override
    public LeaderboardEventInitialization map(InitLeaderboardCreateEvent event) {
         return LeaderboardEventInitialization.builder()
                .lbId(event.getLbId())
                .events(event.getEvents())
                .userId(event.getOwnerId())
                .isPublic(event.isPublic())
                .metadata(Map.of("expire_date", event.getExpireAt()))
                .build();
    }
}
