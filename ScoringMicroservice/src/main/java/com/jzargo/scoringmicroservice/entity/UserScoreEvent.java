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
@Table(name="user_score_events")
public class UserScoreEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String lbId;
    private Double scoreChange;
    @Builder.Default
    private LocalDateTime happenedAt = LocalDateTime.now();
    private String reason;
}
