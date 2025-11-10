package com.jzargo.scoringmicroservice.repository;

import com.jzargo.scoringmicroservice.entity.DeletedEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeletedEventsRepository extends JpaRepository<DeletedEvents, Integer> {
}
