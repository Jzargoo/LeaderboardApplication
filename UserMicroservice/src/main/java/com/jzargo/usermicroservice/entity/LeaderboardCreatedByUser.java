package com.jzargo.usermicroservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "created_leaderboard_outbox")
public class LeaderboardCreatedByUser {
    @Id
    private String id;
    private Long userId;
    private String lbId;
    private String sagaId;
}
