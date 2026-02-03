package com.jzargo.leaderboardmicroservice.mapper;

import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.mapper.Mapper;
import com.jzargo.messaging.InitLeaderboardCreateEvent;
import org.springframework.stereotype.Component;

@Component
public class CreateInitialCreateLeaderboardSagaRequestMapper implements Mapper<CreateLeaderboardRequest, InitLeaderboardCreateEvent> {
    @Override
    public InitLeaderboardCreateEvent map(CreateLeaderboardRequest from) {
        return InitLeaderboardCreateEvent.builder()
                .regions(from.getRegions())
                .initialValue(from.getInitialValue())
                .events(from.getEvents())
                .expireAt(from.getExpireAt())
                .description(from.getDescription())
                .expireAt(from.getExpireAt())
                .globalRange(from.getGlobalRange())
                .regions(from.getRegions())
                .isMutable(from.isMutable())
                .isPublic(from.isPublic())
                .showTies(from.isShowTies())
                .nameLb(from.getName())
                .maxEventsPerUser(from.getMaxEventsPerUser())
                .maxEventsPerUserPerDay(from.getMaxEventsPerUserPerDay())
                .maxScore(from.getMaxScore())
                .build();
    }
}
