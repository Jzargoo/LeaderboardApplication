package com.jzargo.scoringmicroservice.repository;

import com.jzargo.scoringmicroservice.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
    boolean existsById(String messageId);

    void save(ProcessedMessage command);
}
