package com.jzargo.leaderboardmicroservice.mapper;

import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.mapper.Mapper;
import com.jzargo.region.Regions;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class MapperCreateLeaderboardInfo implements Mapper<InitLeaderboardCreateEvent, LeaderboardInfo> {
    @Override
    public LeaderboardInfo map(InitLeaderboardCreateEvent from) {
        UUID uuid = UUID.randomUUID();
        LeaderboardInfo build = LeaderboardInfo.builder()
                .id(uuid.toString())
                .description(from.getDescription())
                .name(from.getNameLb())
                .description(from.getDescription())
                .initialValue(from.getInitialValue())
                .isPublic(from.isPublic())
                .globalRange(from.getGlobalRange())
                .expireAt(from.getExpireAt())
                .maxEventsPerUser(from.getMaxEventsPerUser())
                .maxEventsPerUserPerDay(from.getMaxEventsPerUserPerDay())
                .maxScore(from.getMaxScore())
                .isMutable(from.isMutable())
                .showTies(from.isShowTies())
                .build();
        if(from.getRegions() !=null) {
            build.setRegions(from.getRegions());
        } else {
            build.setRegions(Set.of(Regions.GLOBAL.getCode()));
        }
        return build;

    }
}
