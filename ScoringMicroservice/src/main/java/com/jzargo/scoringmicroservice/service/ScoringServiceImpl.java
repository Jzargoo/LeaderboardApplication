package com.jzargo.scoringmicroservice.service;

import com.jzargo.scoringmicroservice.entity.LeaderboardEvents;
import com.jzargo.scoringmicroservice.entity.ScoringEvent;
import com.jzargo.scoringmicroservice.entity.UserScoreEvent;
import com.jzargo.scoringmicroservice.mapper.LeaderboardCreateMapper;
import com.jzargo.scoringmicroservice.repository.LeaderboardEventsRepository;
import com.jzargo.scoringmicroservice.mapper.CreateHappenedToScoreEventMapper;
import com.jzargo.scoringmicroservice.repository.ScoringEventRepository;
import com.jzargo.scoringmicroservice.repository.UserScoreEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.jzargo.messaging.LeaderboardEventDeletion;
import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.UserEventHappenedCommand;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScoringServiceImpl implements ScoringService{
    private final CreateHappenedToScoreEventMapper createHappenedToScoreEventMapper;
    private final LeaderboardEventsRepository leaderboardEventsRepository;
    private final UserScoreEventRepository userScoreEventRepository;
    private final LeaderboardCreateMapper leaderboardCreateMapper;
    private final ScoringEventRepository scoringEventRepository;

    public ScoringServiceImpl(CreateHappenedToScoreEventMapper createHappenedToScoreEventMapper,
                              LeaderboardEventsRepository leaderboardEventsRepository, UserScoreEventRepository userScoreEventRepository, LeaderboardCreateMapper leaderboardCreateMapper, ScoringEventRepository scoringEventRepository) {
        this.createHappenedToScoreEventMapper = createHappenedToScoreEventMapper;
        this.leaderboardEventsRepository = leaderboardEventsRepository;
        this.userScoreEventRepository = userScoreEventRepository;
        this.leaderboardCreateMapper = leaderboardCreateMapper;
        this.scoringEventRepository = scoringEventRepository;
    }

    @Transactional
    @Override
    public void saveUserEvent(UserEventHappenedCommand message) {
        UserScoreEvent map = createHappenedToScoreEventMapper.map(message);
        var events = leaderboardEventsRepository.findById(message.getLbId())
                .orElseThrow().getEvents();
        events.stream().filter(event -> event.getEventName().equals(map.getReason()))
                .findFirst()
                .ifPresentOrElse(
                        (event) -> map.setScoreChange(event.getScore()),
                        () -> {
                            throw new IllegalArgumentException("Event not found");
                        }
                );

        userScoreEventRepository.save(map);
        log.info("Saved user event for user {} on leaderboard {}", map.getUserId(), map.getLbId());
    }

    @Override
    @Transactional
    public void saveEvents(LeaderboardEventInitialization message) {
        LeaderboardEvents map = leaderboardCreateMapper.map(message);
        message.getEvents().forEach((key, value) -> scoringEventRepository.getScoringEventByEventNameAndScore(key, value)
                .ifPresentOrElse(
                        map::addEvent,
                        () -> {
                            ScoringEvent scoringEvent = ScoringEvent.builder()
                                    .eventName(key)
                                    .score(value)
                                    .build();
                            scoringEventRepository.save(scoringEvent);
                            map.getEvents().add(scoringEvent);
                        }
                ));

        LeaderboardEvents save = leaderboardEventsRepository.save(map);
        if(save.getEvents().size() != message.getEvents().size()){
            log.error("Not all events were saved for leaderboard {}", map.getId());
            throw new IllegalStateException("Not all events were saved");
        }
        log.info("Saved events for leaderboard {}", map.getId());
    }

    @Override
    @Transactional
    public boolean deleteEvents(LeaderboardEventDeletion message) {
        if (leaderboardEventsRepository.existsById(message.getLbId())) {
            leaderboardEventsRepository.deleteById(message.getLbId());
        }
        return leaderboardEventsRepository.existsById(message.getLbId());
    }
}