package com.jzargo.scoringmicroservice.repository;

import com.jzargo.scoringmicroservice.entity.ScoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoringEventRepository extends JpaRepository<ScoringEvent, Long> {
    Optional<ScoringEvent> getScoringEventByEventNameAndScore(String eventName, Double score);
}
