package com.jzargo.leaderboardmicroservice.repository;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface LeaderboardInfoRepository extends CrudRepository<LeaderboardInfo,String>{
}
