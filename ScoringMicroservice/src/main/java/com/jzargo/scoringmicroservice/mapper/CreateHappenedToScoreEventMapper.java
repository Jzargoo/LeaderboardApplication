package com.jzargo.scoringmicroservice.mapper;

import com.jzargo.scoringmicroservice.entity.UserScoreEvent;
import mapper.Mapper;
import messaging.UserEventHappenedCommand;
import org.springframework.stereotype.Component;

@Component
public class CreateHappenedToScoreEventMapper implements Mapper<UserEventHappenedCommand, UserScoreEvent> {
    @Override
    public UserScoreEvent map(UserEventHappenedCommand from) {
        return UserScoreEvent.builder()
                .userId(from.getUserId())
                .reason(from.getEventName())
                .lbId(from.getLbId())
                .build();
    }
}
