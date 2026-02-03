package com.jzargo.scoringmicroservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor @NoArgsConstructor
@Builder
@Data
@Table(name="scoring_events")
public class ScoringEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String lbId;
    private String eventName;
    @Column(name="event_score")
    private Double scoreChange;
    @Builder.Default
    private LocalDateTime happenedAt = LocalDateTime.now();

}
