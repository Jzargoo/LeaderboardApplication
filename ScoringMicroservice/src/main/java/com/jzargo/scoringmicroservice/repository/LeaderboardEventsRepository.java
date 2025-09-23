package com.jzargo.scoringmicroservice.repository;

import com.jzargo.scoringmicroservice.entity.LeaderboardEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaderboardEventsRepository extends JpaRepository<LeaderboardEvents, String> {
}
