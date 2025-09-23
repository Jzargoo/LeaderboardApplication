package com.jzargo.scoringmicroservice.repository;

import com.jzargo.scoringmicroservice.entity.UserScoreEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserScoreEventRepository extends JpaRepository<UserScoreEvent, Long> {
}
