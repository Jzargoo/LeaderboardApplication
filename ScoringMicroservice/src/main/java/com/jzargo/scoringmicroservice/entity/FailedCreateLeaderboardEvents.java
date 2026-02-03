package com.jzargo.scoringmicroservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "failed_create_leaderboard_events")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FailedCreateLeaderboardEvents {
    @Id
    private String id;
    @Column(name = "leaderboard_id")
    private String lbId;
    private String reason;
    private String sagaId;
    private long userId;
}
