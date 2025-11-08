package com.jzargo.usermicroservice.repository;

import com.jzargo.usermicroservice.entity.UserAddedCreatedLeaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAddedCreatedLeaderboardRepository extends JpaRepository<UserAddedCreatedLeaderboard, String> {
}
