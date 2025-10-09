package com.jzargo.usermicroservice.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor @NoArgsConstructor
@Builder
@Table(name = "processing_messages")
public class ProcessingMessage {
    @Id
    private String id;
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
    @Column(name="message_type")
    private String type;
}
