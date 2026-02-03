package com.jzargo.usermicroservice.repository;

import com.jzargo.usermicroservice.entity.ProcessingMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessingMessageRepository extends JpaRepository<ProcessingMessage, String> {
}
