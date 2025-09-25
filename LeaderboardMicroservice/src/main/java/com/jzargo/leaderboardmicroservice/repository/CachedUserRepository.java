package com.jzargo.leaderboardmicroservice.repository;

import com.jzargo.leaderboardmicroservice.entity.UserCached;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CachedUserRepository extends CrudRepository<UserCached, Long> {
}
