package com.jzargo.scoringmicroservice.repository;

import com.jzargo.scoringmicroservice.entity.LbEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LbEventTypeRepository extends JpaRepository<LbEventType, Long> {
    Optional<LbEventType> findByEventNameAndScore(String name, Double score);
}
