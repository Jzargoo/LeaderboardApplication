package com.jzargo.usermicroservice.repository;

import com.jzargo.usermicroservice.entity.LeaderboardCreatedByUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaderboardCreatedByUserRepository extends JpaRepository<LeaderboardCreatedByUser, String> {
}
