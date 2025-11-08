package com.jzargo.scoringmicroservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "successful_events_creation")
public class SuccessfulEventsCreation {
        @Id
        private String id;
        @Column(name = "leaderboard_id")
        private String lbId;
        private String sagaId;
        private long userId;
}
