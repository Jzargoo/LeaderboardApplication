package com.jzargo.leaderboardmicroservice.mapper;

import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.mapper.Mapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MapperCreateLeaderboardInfo implements Mapper<CreateLeaderboardRequest, LeaderboardInfo> {
    @Override
    public LeaderboardInfo map(CreateLeaderboardRequest from) {
        UUID uuid = UUID.randomUUID();
        LeaderboardInfo build = LeaderboardInfo.builder()
                .description(uuid.toString())
                .name(from.getName())
                .description(from.getDescription())
                .initialValue(from.getInitialValue())
                .isPublic(from.isPublic())
                .globalRange(from.getGlobalRange())
                .maxEventsPerUser(from.getMaxEventsPerUser())
                .maxEventsPerUserPerDay(from.getMaxEventsPerUserPerDay())
                .maxScore(from.getMaxScore())
                .isMutable(from.isMutable())
                .build();
        if(from.getRegions() !=null) {
            build.setRegions(from.getRegions());
        }
        return build;

    }
}
