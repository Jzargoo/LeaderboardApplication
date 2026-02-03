package com.jzargo.scoringmicroservice.mapper;

import com.jzargo.mapper.Mapper;
import com.jzargo.messaging.UserEventHappenedCommand;
import com.jzargo.scoringmicroservice.entity.ScoringEvent;
import org.springframework.stereotype.Component;

@Component
public class CreateHappenedToScoreEventMapper implements Mapper<UserEventHappenedCommand, ScoringEvent> {

    @Override
    public ScoringEvent map(UserEventHappenedCommand from) {
        return ScoringEvent.builder()
                .userId(from.getUserId())
                .eventName(from.getEventName())
                .lbId(from.getLbId())
                .build();
    }
}
