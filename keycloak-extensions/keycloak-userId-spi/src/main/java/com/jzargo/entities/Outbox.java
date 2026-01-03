package com.jzargo.entities;

import jakarta.persistence.*;
import lombok.*;
import org.keycloak.events.EventType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outbox")
public class Outbox {
    @Id
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private UUID id = UUID.randomUUID();
    @Column(name = "aggregate_type")
    private String aggregateType;
    @Column(name = "aggregate_id")
    private String aggregateId;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;
    @Column(name = "payload", nullable = false)
    private String payload;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Builder.Default
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
