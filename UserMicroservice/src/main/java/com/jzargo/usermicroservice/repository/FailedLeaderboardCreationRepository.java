package com.jzargo.usermicroservice.repository;

import com.jzargo.usermicroservice.entity.FailedLeaderboardCreation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedLeaderboardCreationRepository extends JpaRepository<FailedLeaderboardCreation, Integer> {
}
