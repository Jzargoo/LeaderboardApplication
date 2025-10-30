package com.jzargo.scoringmicroservice.service;

import com.jzargo.scoringmicroservice.entity.LbEventType;
import com.jzargo.scoringmicroservice.entity.LeaderboardEvents;
import com.jzargo.scoringmicroservice.entity.ScoringEvent;
import com.jzargo.scoringmicroservice.mapper.LeaderboardCreateMapper;
import com.jzargo.scoringmicroservice.repository.LbEventTypeRepository;
import com.jzargo.scoringmicroservice.repository.LeaderboardEventsRepository;
import com.jzargo.scoringmicroservice.mapper.CreateHappenedToScoreEventMapper;
import com.jzargo.scoringmicroservice.repository.ScoringEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.jzargo.messaging.LeaderboardEventDeletion;
import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.UserEventHappenedCommand;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class ScoringServiceImpl implements ScoringService{
    private final CreateHappenedToScoreEventMapper createHappenedToScoreEventMapper;
    private final LeaderboardEventsRepository leaderboardEventsRepository;
    private final LbEventTypeRepository lbEventTypeRepository;
    private final LeaderboardCreateMapper leaderboardCreateMapper;
    private final ScoringEventRepository scoringEventRepository;

    public ScoringServiceImpl(CreateHappenedToScoreEventMapper createHappenedToScoreEventMapper,
                              LeaderboardEventsRepository leaderboardEventsRepository, LbEventTypeRepository lbEventTypeRepository,
                              LeaderboardCreateMapper leaderboardCreateMapper, ScoringEventRepository scoringEventRepository) {
        this.createHappenedToScoreEventMapper = createHappenedToScoreEventMapper;
        this.leaderboardEventsRepository = leaderboardEventsRepository;
        this.lbEventTypeRepository = lbEventTypeRepository;
        this.leaderboardCreateMapper = leaderboardCreateMapper;
        this.scoringEventRepository = scoringEventRepository;
    }

    @Transactional
    @Override
    public void saveUserEvent(UserEventHappenedCommand message) {
        ScoringEvent map = createHappenedToScoreEventMapper.map(message);
        var events = leaderboardEventsRepository.findById(message.getLbId())
                .orElseThrow().getEvents();
        events.stream().filter(event -> event.getEventName().equals(map.getEventName()))
                .findFirst()
                .ifPresentOrElse(
                        (event) -> map.setScoreChange(event.getScore()),
                        () -> {
                            throw new IllegalArgumentException("Event not found");
                        }
                );

        scoringEventRepository.save(map);
        log.info("Saved user event for user {} on leaderboard {}", map.getUserId(), map.getLbId());
    }

    @Override
    @Transactional
    public void saveEvents(LeaderboardEventInitialization message) {
        LeaderboardEvents map = leaderboardCreateMapper.map(message);

        long count = message.getEvents()
                .entrySet().stream()
                .peek(entry -> {
                    if (entry.getValue() == null || entry.getKey() == null) {
                        log.error("Event name or score is null for leaderboard {}", map.getId());
                        throw new IllegalArgumentException("Event name or score is null");
                    }
                })
                .peek((entry) -> {
                    Optional<LbEventType> optionalEvent = lbEventTypeRepository
                            .findByEventNameAndScore(entry.getKey(), entry.getValue());
                    if (optionalEvent.isEmpty()) {
                                LbEventType build = LbEventType.builder()
                                        .eventName(entry.getKey())
                                        .score(entry.getValue())
                                        .build();
                                lbEventTypeRepository.saveAndFlush(build);
                                map.addEvent(build);
                            } else {
                                map.addEvent(optionalEvent.get());
                            }
                })
                .count();
        if (count != message.getEvents().size()) {
            log.error("Not all events were processed for leaderboard {}", map.getId());
            throw new IllegalStateException("Not all events were processed");
        }

        leaderboardEventsRepository.save(map);
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