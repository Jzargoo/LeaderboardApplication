package com.jzargo.scoringmicroservice.mapper;

import com.jzargo.scoringmicroservice.entity.LeaderboardEvents;
import mapper.Mapper;
import messaging.LeaderboardEventInitialization;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardCreateMapper implements Mapper<LeaderboardEventInitialization, LeaderboardEvents> {
    @Override
    public LeaderboardEvents map(LeaderboardEventInitialization from) {
        return LeaderboardEvents.builder()
                .id(from.getLbId())
                .isPublic(from.isPublic())
                .metadata(from.getMetadata().toString())
                .build();
    }
}
