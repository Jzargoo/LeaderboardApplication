package com.jzargo.scoringmicroservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Table(name = "processed_messages")
@Entity
@Data
@AllArgsConstructor @NoArgsConstructor
public class ProcessedMessage {
    @Id
    private String id;

    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();

    private String messageType;
}