package com.jzargo.leaderboardmicroservice.repository;

import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaControllingStateRepository extends CrudRepository<SagaControllingState, String> {
}
